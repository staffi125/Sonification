package org.example.sonificationmultimedia.sonification;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Assembles a {@link Sequence} from a list of {@link MidiNote} objects and writes
 * it to a Standard MIDI File (.mid).
 *
 * <h2>MIDI File Structure</h2>
 * <ul>
 *   <li>Format type 0 (single track)</li>
 *   <li>Resolution: {@value #TICKS_PER_BEAT} ticks per quarter-note beat</li>
 *   <li>Tempo: {@value #BPM} BPM (set via a Tempo meta-event)</li>
 *   <li>Notes play sequentially (one after another, no overlap)</li>
 *   <li>Each note is preceded by a Program Change event if its instrument
 *       differs from the previous note's instrument, keeping the file compact.</li>
 * </ul>
 *
 * <h2>Event types used</h2>
 * <ul>
 *   <li>0xFF 0x51 — Set Tempo meta-event</li>
 *   <li>0xC0       — Program Change (instrument selection)</li>
 *   <li>0x90       — Note On</li>
 *   <li>0x80       — Note Off</li>
 * </ul>
 */
public class MidiComposer {

    /** Ticks per quarter-note beat (standard high resolution). */
    public static final int TICKS_PER_BEAT = 480;

    /** Beats per minute for the generated composition. */
    public static final int BPM = 120;

    /**
     * Builds a MIDI {@link Sequence} from the supplied list of notes and writes
     * it to the given output file.
     *
     * @param notes      ordered list of MIDI notes (one per data row)
     * @param outputFile destination file (created or overwritten)
     * @throws MidiUnavailableException if the MIDI system is unavailable
     * @throws InvalidMidiDataException if note parameters are out of range
     * @throws IOException              if the file cannot be written
     */
    public void compose(List<MidiNote> notes, File outputFile)
            throws MidiUnavailableException, InvalidMidiDataException, IOException {

        // Create sequence: PPQ timing, 480 ticks/beat
        Sequence sequence = new Sequence(Sequence.PPQ, TICKS_PER_BEAT);
        Track track = sequence.createTrack();

        // ── Tempo meta-event at tick 0 ────────────────────────────────────────
        int microsPerBeat = 60_000_000 / BPM;
        MetaMessage tempoMsg = new MetaMessage(
                0x51,
                new byte[]{
                        (byte) ((microsPerBeat >> 16) & 0xFF),
                        (byte) ((microsPerBeat >> 8)  & 0xFF),
                        (byte) (microsPerBeat         & 0xFF)
                },
                3
        );
        track.add(new MidiEvent(tempoMsg, 0));

        // ── Note events ───────────────────────────────────────────────────────
        long currentTick = 0;
        int lastInstrument = -1;   // track instrument changes

        for (MidiNote note : notes) {
            int channel    = note.getChannel();
            int instrument = note.getInstrument();

            // Program Change only when instrument changes (saves space)
            if (instrument != lastInstrument) {
                ShortMessage programChange = new ShortMessage();
                programChange.setMessage(ShortMessage.PROGRAM_CHANGE, channel, instrument, 0);
                track.add(new MidiEvent(programChange, currentTick));
                lastInstrument = instrument;
            }

            long noteDurationTicks = Math.round(note.getDurationBeats() * TICKS_PER_BEAT);

            // Note On
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(ShortMessage.NOTE_ON, channel, note.getPitch(), note.getVelocity());
            track.add(new MidiEvent(noteOn, currentTick));

            // Note Off (placed at end of note, with a tiny gap for articulation)
            long noteOffTick = currentTick + noteDurationTicks - 10;
            if (noteOffTick <= currentTick) noteOffTick = currentTick + 1;

            ShortMessage noteOff = new ShortMessage();
            noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note.getPitch(), 0);
            track.add(new MidiEvent(noteOff, noteOffTick));

            currentTick += noteDurationTicks;
        }

        // ── End-of-track meta-event ───────────────────────────────────────────
        MetaMessage endOfTrack = new MetaMessage(0x2F, new byte[]{}, 0);
        track.add(new MidiEvent(endOfTrack, currentTick));

        // ── Write to file ─────────────────────────────────────────────────────
        outputFile.getParentFile().mkdirs();
        MidiSystem.write(sequence, 0, outputFile);
    }


    public void printSummary(List<MidiNote> notes, List<String> columnNames) {
        System.out.println();
        System.out.println("+-------+----------+----------+----------+--------------------+");
        System.out.println("|           MIDI Sonification - Generated Notes               |");
        System.out.println("+-------+----------+----------+----------+--------------------+");
        System.out.printf( "| %-5s | %-8s | %-8s | %-8s | %-18s |%n",
                "#", "Note", "Velocity", "Duration", "Instrument (GM)");
        System.out.println("+-------+----------+----------+----------+--------------------+");

        for (int i = 0; i < notes.size(); i++) {
            MidiNote n = notes.get(i);
            System.out.printf("| %-5d | %-8s | %-8d | %-8.2f | %-18s |%n",
                    i + 1,
                    MidiNote.noteName(n.getPitch()),
                    n.getVelocity(),
                    n.getDurationBeats(),
                    gmInstrumentName(n.getInstrument()));
        }
        System.out.println("+-------+----------+----------+----------+--------------------+");
        System.out.printf("%nTotal notes: %d  |  BPM: %d  |  Resolution: %d ticks/beat%n%n",
                notes.size(), BPM, TICKS_PER_BEAT);
    }

    /** Returns a short display name for common General MIDI program numbers. */
    private static String gmInstrumentName(int program) {
        return switch (program) {
            case 0   -> "Grand Piano";
            case 24  -> "Nylon Guitar";
            case 40  -> "Violin";
            case 48  -> "String Ensemble";
            case 56  -> "Trumpet";
            case 65  -> "Alto Sax";
            case 73  -> "Flute";
            case 89  -> "Warm Pad";
            default  -> "GM #" + program;
        };
    }
}
