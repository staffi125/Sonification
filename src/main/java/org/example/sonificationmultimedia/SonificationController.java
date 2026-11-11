package org.example.sonificationmultimedia;

import org.example.sonificationmultimedia.sonification.CsvDataLoader;
import org.example.sonificationmultimedia.sonification.DataRecord;
import org.example.sonificationmultimedia.sonification.SonificationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST controller exposing the MIDI Sonification API.
 *69
 * <pre>
 * POST /api/preview        multipart CSV  → JSON note table
 * POST /api/compose        multipart CSV  → .mid file download
 * GET  /api/sample-csv     (no body)      → downloads bundled sample_data.csv
 * POST /api/preview-raw    plain CSV text → JSON note table  (used by manual editor)
 * POST /api/compose-raw    plain CSV text → .mid file download
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class SonificationController {

    private final SonificationService service;

    public SonificationController(SonificationService service) {
        this.service = service;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> previewUpload(@RequestParam("file") MultipartFile file) {
        try {
            CsvDataLoader loader = new CsvDataLoader();
            List<DataRecord> records = loader.loadFromStream(file.getInputStream());
            List<String> cols = loader.getColumnNames();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("columns", cols);
            body.put("recordCount", records.size());
            body.put("stats", service.stats(records, cols));
            body.put("notes", service.preview(records, cols));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }



    @PostMapping(value = "/compose", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> composeUpload(@RequestParam("file") MultipartFile file) {
        try {
            CsvDataLoader loader = new CsvDataLoader();
            List<DataRecord> records = loader.loadFromStream(file.getInputStream());
            List<String> cols = loader.getColumnNames();

            byte[] midi = service.compose(records, cols);
            return midiResponse(midi, "composition.mid");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping(value = "/preview-raw", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> previewRaw(@RequestBody String csvText) {
        try {
            CsvDataLoader loader = new CsvDataLoader();
            List<DataRecord> records = loader.loadFromString(csvText);
            List<String> cols = loader.getColumnNames();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("columns", cols);
            body.put("recordCount", records.size());
            body.put("stats", service.stats(records, cols));
            body.put("notes", service.preview(records, cols));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping(value = "/compose-raw", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> composeRaw(@RequestBody String csvText) {
        try {
            CsvDataLoader loader = new CsvDataLoader();
            List<DataRecord> records = loader.loadFromString(csvText);
            List<String> cols = loader.getColumnNames();

            byte[] midi = service.compose(records, cols);
            return midiResponse(midi, "composition.mid");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }



    @GetMapping("/sample-csv")
    public ResponseEntity<?> sampleCsv() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("sample_data.csv")) {
            if (is == null) return ResponseEntity.notFound().build();
            byte[] bytes = is.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"sample_data.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    private ResponseEntity<byte[]> midiResponse(byte[] midi, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("audio/midi"))
                .body(midi);
    }
}
