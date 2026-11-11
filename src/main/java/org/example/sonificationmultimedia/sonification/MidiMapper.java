package org.example.sonificationmultimedia.sonification;

import java.util.ArrayList;
import java.util.List;

public class MidiMapper {

    // ── Pitch output range ────────────────────────────────────────────────────
    private static final int PITCH_MIN = 48;   // C3
    private static final int PITCH_MAX = 84;   // C6

    // ── Velocity output range ─────────────────────────────────────────────────
    private static final int VEL_MIN = 40;
    private static final int VEL_MAX = 120;

    // ── Duration output range (quarter-note beats) ────────────────────────────
    private static final double DUR_MIN = 0.25;
    private static final double DUR_MAX = 1.5;

    /**
     * Curated General MIDI instrument palette (8 timbres).
     * Spread evenly across the fourth column's data range.
     *
     * GM program numbers: 0-based.
     *   0  = Acoustic Grand Piano
     *  24  = Acoustic Guitar (nylon)
     *  40  = Violin
     *  48  = String Ensemble 1
     *  56  = Trumpet
     *  65  = Alto Sax
     *  73  = Flute
     *  89  = Pad 2 (warm)
     */
    private static final int[] INSTRUMENT_PALETTE = {0, 24, 40, 48, 56, 65, 73, 89};

    // Per-column statistics
    private final double[] colMin;
    private final double[] colMax;
    private final int numColumns;

    /**
     * Constructs a mapper by scanning the full dataset to find per-column min/max.
     *
     * @param records full list of data records (at least one row, two columns)
     */
    public MidiMapper(List<DataRecord> records) {
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Record list must not be empty.");
        }
        numColumns = records.get(0).size();
        colMin = new double[numColumns];
        colMax = new double[numColumns];

        // Initialise with first row
        DataRecord first = records.get(0);
        for (int c = 0; c < numColumns; c++) {
            colMin[c] = first.get(c);
            colMax[c] = first.get(c);
        }

        // Scan remainder
        for (int i = 1; i < records.size(); i++) {
            DataRecord rec = records.get(i);
            for (int c = 0; c < numColumns; c++) {
                double v = rec.get(c);
                if (v < colMin[c]) colMin[c] = v;
                if (v > colMax[c]) colMax[c] = v;
            }
        }
    }


    public MidiNote map(DataRecord record) {
        // Rule 1: Column 0 → Pitch
        int pitch = (int) Math.round(
                linearMap(record.get(0), colMin[0], colMax[0], PITCH_MIN, PITCH_MAX));

        // Rule 2: Column 1 → Velocity
        int velocity = (int) Math.round(
                linearMap(record.get(1), colMin[1], colMax[1], VEL_MIN, VEL_MAX));

        // Rule 3: Column 2 → Duration (optional)
        double duration = DUR_MIN + (DUR_MAX - DUR_MIN) / 2.0; // default: 0.875 beats
        if (numColumns >= 3) {
            duration = linearMap(record.get(2), colMin[2], colMax[2], DUR_MIN, DUR_MAX);
        }

        // Rule 4: Column 3 → Instrument from palette (optional)
        int instrument = INSTRUMENT_PALETTE[0]; // default: Grand Piano
        if (numColumns >= 4) {
            int paletteIndex = (int) Math.round(
                    linearMap(record.get(3), colMin[3], colMax[3], 0, INSTRUMENT_PALETTE.length - 1));
            instrument = INSTRUMENT_PALETTE[clamp(paletteIndex, 0, INSTRUMENT_PALETTE.length - 1)];
        }

        return new MidiNote(pitch, velocity, duration, instrument, 0);
    }

    /**
     * Convenience method: maps an entire list of records.
     *
     * @param records input data
     * @return list of MIDI notes in the same order
     */
    public List<MidiNote> mapAll(List<DataRecord> records) {
        List<MidiNote> notes = new ArrayList<>(records.size());
        for (DataRecord r : records) {
            notes.add(map(r));
        }
        return notes;
    }

    /** Returns the per-column minimum values discovered during construction. */
    public double[] getColumnMin() { return colMin.clone(); }

    /** Returns the per-column maximum values discovered during construction. */
    public double[] getColumnMax() { return colMax.clone(); }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Linear interpolation: maps {@code value} from [srcMin, srcMax] to [dstMin, dstMax].
     * If srcMin == srcMax (constant column), returns the midpoint of the destination range.
     */
    static double linearMap(double value, double srcMin, double srcMax,
                            double dstMin, double dstMax) {
        if (srcMax == srcMin) {
            return (dstMin + dstMax) / 2.0;
        }
        double t = (value - srcMin) / (srcMax - srcMin);
        t = Math.max(0.0, Math.min(1.0, t)); // clamp to [0,1]
        return dstMin + t * (dstMax - dstMin);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
