package org.example.sonificationmultimedia.sonification;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class SonificationService {

    private static final String[] GM_NAMES = {
            "Grand Piano", "Nylon Guitar", "Violin",
            "String Ensemble", "Trumpet", "Alto Sax", "Flute", "Warm Pad"
    };

    public List<Map<String, Object>> preview(List<DataRecord> records,
                                             List<String> columnNames) {
        MidiMapper mapper = new MidiMapper(records);
        List<MidiNote> notes = mapper.mapAll(records);

        return notes.stream().map(n -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("row",        notes.indexOf(n) + 1);
            row.put("note",       MidiNote.noteName(n.getPitch()));
            row.put("pitch",      n.getPitch());
            row.put("velocity",   n.getVelocity());
            row.put("duration",   Math.round(n.getDurationBeats() * 100.0) / 100.0);
            row.put("instrument", gmName(n.getInstrument()));
            row.put("channel",    n.getChannel());
            return row;
        }).toList();
    }

    public byte[] compose(List<DataRecord> records,
                          List<String> columnNames) throws Exception {
        MidiMapper   mapper   = new MidiMapper(records);
        List<MidiNote> notes  = mapper.mapAll(records);
        MidiComposer composer = new MidiComposer();

        File tmp = File.createTempFile("sonification-", ".mid");
        tmp.deleteOnExit();
        try {
            composer.compose(notes, tmp);
            return Files.readAllBytes(tmp.toPath());
        } finally {
            tmp.delete();
        }
    }


    public Map<String, Object> stats(List<DataRecord> records,
                                     List<String> columnNames) {
        MidiMapper mapper = new MidiMapper(records);
        double[] min = mapper.getColumnMin();
        double[] max = mapper.getColumnMax();

        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.size(); i++) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("min", Math.round(min[i] * 100.0) / 100.0);
            col.put("max", Math.round(max[i] * 100.0) / 100.0);
            result.put(columnNames.get(i), col);
        }
        return result;
    }

    private static String gmName(int program) {
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
