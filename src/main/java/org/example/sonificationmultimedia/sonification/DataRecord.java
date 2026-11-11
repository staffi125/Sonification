package org.example.sonificationmultimedia.sonification;

import java.util.Collections;
import java.util.List;


public class DataRecord {

    private final int rowIndex;
    private final List<Double> values;

    public DataRecord(int rowIndex, List<Double> values) {
        this.rowIndex = rowIndex;
        this.values = Collections.unmodifiableList(values);
    }

    /** Zero-based row index in the original CSV (excluding header). */
    public int getRowIndex() {
        return rowIndex;
    }

    /** All numerical column values for this row. */
    public List<Double> getValues() {
        return values;
    }

    /**
     * Returns the value at the given column index, or {@code defaultValue}
     * if the column does not exist.
     */
    public double get(int columnIndex, double defaultValue) {
        if (columnIndex < 0 || columnIndex >= values.size()) {
            return defaultValue;
        }
        return values.get(columnIndex);
    }

    /** Shorthand for {@code get(columnIndex, 0.0)}. */
    public double get(int columnIndex) {
        return get(columnIndex, 0.0);
    }

    /** Number of numerical columns in this record. */
    public int size() {
        return values.size();
    }

    @Override
    public String toString() {
        return "DataRecord{row=" + rowIndex + ", values=" + values + "}";
    }
}
