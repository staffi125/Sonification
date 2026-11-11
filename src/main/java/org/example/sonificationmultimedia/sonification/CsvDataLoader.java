package org.example.sonificationmultimedia.sonification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class CsvDataLoader {

    private List<String> columnNames = new ArrayList<>();


    public List<DataRecord> loadFromFile(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        }
    }


    public List<DataRecord> loadFromClasspath(String resourceName) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new IOException("Classpath resource not found: " + resourceName);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return parse(reader);
        }
    }

    /** Returns the header column names discovered during the last load call. */
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }


    public List<DataRecord> loadFromStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return parse(reader);
        }
    }


    public List<DataRecord> loadFromString(String csvText) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new java.io.StringReader(csvText))) {
            return parse(reader);
        }
    }


    // Private helpers


    private List<DataRecord> parse(BufferedReader reader) throws IOException {
        columnNames = new ArrayList<>();
        List<DataRecord> records = new ArrayList<>();

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IOException("CSV file is empty.");
        }

        // Parse header — strip BOM if present
        String cleanHeader = headerLine.startsWith("\uFEFF")
                ? headerLine.substring(1) : headerLine;
        columnNames = Arrays.stream(cleanHeader.split(",", -1))
                .map(String::trim)
                .collect(Collectors.toList());

        int rowIndex = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(",", -1);
            List<Double> values = new ArrayList<>();
            boolean valid = true;

            for (String part : parts) {
                try {
                    values.add(Double.parseDouble(part.trim()));
                } catch (NumberFormatException e) {
                    // Non-numeric cell — skip this entire row
                    valid = false;
                    break;
                }
            }

            if (valid && values.size() >= 2) {
                records.add(new DataRecord(rowIndex, values));
                rowIndex++;
            }
        }

        if (records.isEmpty()) {
            throw new IOException("No valid numeric rows found in CSV (need at least 2 columns).");
        }

        return records;
    }
}
