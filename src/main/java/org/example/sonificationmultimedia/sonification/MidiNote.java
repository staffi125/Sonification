package org.example.sonificationmultimedia.sonification;

/**
 * Immutable value object representing a single MIDI note event with all
 * parameters needed to write it to a MIDI sequence.
 *
 * <p>Parameter ranges:
 * <ul>
 *   <li>{@code pitch}      — MIDI note number 0–127 (middle C = 60)</li>
 *   <li>{@code velocity}   — MIDI velocity 0–127 (0 = silent, 127 = maximum)</li>
 *   <li>{@code durationBeats} — note length in quarter-note beats (e.g. 0.5 = eighth note)</li>
 *   <li>{@code instrument} — General MIDI program number 0–127</li>
 *   <li>{@code channel}    — MIDI channel 0–15 (channel 9 is reserved for drums)</li>
 * </ul>
 */
public class MidiNote {

    private final int pitch;
    private final int velocity;
    private final double durationBeats;
    private final int instrument;
    private final int channel;

    public MidiNote(int pitch, int velocity, double durationBeats, int instrument, int channel) {
        this.pitch         = clamp(pitch, 0, 127);
        this.velocity      = clamp(velocity, 0, 127);
        this.durationBeats = Math.max(0.05, durationBeats);
        this.instrument    = clamp(instrument, 0, 127);
        this.channel       = clamp(channel, 0, 15);
    }

    public int getPitch()            { return pitch; }
    public int getVelocity()         { return velocity; }
    public double getDurationBeats() { return durationBeats; }
    public int getInstrument()       { return instrument; }
    public int getChannel()          { return channel; }

    /** Returns the English note name for a MIDI pitch number (e.g. 60 → "C4"). */
    public static String noteName(int midiPitch) {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        int octave = (midiPitch / 12) - 1;
        return names[midiPitch % 12] + octave;
    }

    @Override
    public String toString() {
        return String.format("MidiNote{note=%s, vel=%d, dur=%.2f beats, prog=%d, ch=%d}",
                noteName(pitch), velocity, durationBeats, instrument, channel);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
