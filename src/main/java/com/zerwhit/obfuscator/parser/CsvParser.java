package com.zerwhit.obfuscator.parser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvParser {
    private static final Logger logger = LogManager.getLogger(CsvParser.class);
    
    public Map<String, String> fieldMappings = new HashMap<>();
    public Map<String, String> methodMappings = new HashMap<>();

    public CsvParser(String fieldsCSV, String methodsCSV) throws IOException {
//        loadMappings(fieldsCSV, fieldMappings, true);
//        loadMappings(methodsCSV, methodMappings, false);
    }

    private static void loadMappings(String csvFile, Map<String, String> mappings, boolean isField) throws IOException {
        if (!Files.exists(Paths.get(csvFile))) {
            logger.warn("CSV file not found: {}", csvFile);
            return;
        }

        List<String> lines = Files.readAllLines(Paths.get(csvFile));
        boolean isHeader = true;

        for (String line : lines) {
            if (isHeader) {
                isHeader = false;
                continue;
            }

            String[] parts = parseCSVLine(line);
            if (parts.length >= 2) {
                String searge = parts[0].trim();
                String name = parts[1].trim();

                if (!searge.isEmpty() && !name.isEmpty()) {
                    if (isField) {
                        if (searge.startsWith("field_")) {
                            mappings.put(name, searge);
                        }
                    } else {
                        if (searge.startsWith("func_")) {
                            mappings.put(name, searge);
                        }
                    }
                }
            }
        }
    }

    private static String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        result.add(field.toString());

        return result.toArray(new String[0]);
    }
}