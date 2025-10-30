package com.zerwhit.obfuscator.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TsrgParser {
    private Map<String, String> classMappings = new HashMap<>();
    private Map<String, Map<String, String>> fieldMappings = new HashMap<>();
    private Map<String, Map<String, String>> methodMappings = new HashMap<>();
    private Map<String, Map<String, String>> methodDescriptorMappings = new HashMap<>();

    public void parseTsrgFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        parseTsrgLines(lines);
    }

    public void parseTsrgContent(String content) {
        String[] lines = content.split("\n");
        parseTsrgLines(Arrays.asList(lines));
    }

    private void parseTsrgLines(List<String> lines) {
        String currentClass = null;

        for (String rawLine : lines) {
            String line = rawLine;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            if (!line.startsWith(" ") && !line.startsWith("\t")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    String originalClass = parts[0];
                    String mappedClass = parts[1];
                    classMappings.put(originalClass, mappedClass);
                    currentClass = originalClass;
                }
            } else {
                if (currentClass == null) continue;

                String[] parts = line.trim().split("\\s+", 3);

                if (parts.length == 2) {
                    String originalField = parts[0];
                    String mappedField = parts[1];

                    fieldMappings.computeIfAbsent(currentClass, k -> new HashMap<>())
                            .put(originalField, mappedField);

                } else if (parts.length == 3) {
                    String originalMethod = parts[0];
                    String descriptor = parts[1];
                    String mappedMethod = parts[2];

                    methodMappings.computeIfAbsent(currentClass, k -> new HashMap<>())
                            .put(originalMethod, mappedMethod);
                    methodDescriptorMappings.computeIfAbsent(currentClass, k -> new HashMap<>())
                            .put(originalMethod + descriptor, mappedMethod);
                }
            }
        }
    }

    public String getMappedClass(String originalClass) {
        return classMappings.getOrDefault(originalClass, originalClass);
    }

    public String getMappedField(String className, String fieldName) {
        Map<String, String> classFields = fieldMappings.get(className);
        if (classFields != null) {
            return classFields.getOrDefault(fieldName, fieldName);
        }
        return fieldName;
    }

    public String getMappedMethod(String className, String methodName) {
        Map<String, String> classMethods = methodMappings.get(className);
        if (classMethods != null) {
            return classMethods.getOrDefault(methodName, methodName);
        }
        return methodName;
    }

    public String getMappedMethod(String className, String methodName, String descriptor) {
        String key = methodName + descriptor;
        Map<String, String> classMethods = methodDescriptorMappings.get(className);
        if (classMethods != null && classMethods.containsKey(key)) {
            return classMethods.get(key);
        }

        classMethods = methodMappings.get(className);
        if (classMethods != null && classMethods.containsKey(methodName)) {
            return classMethods.get(methodName);
        }

        return methodName;
    }

    public Map<String, String> getClassMappings() {
        return Collections.unmodifiableMap(classMappings);
    }

    public Map<String, Map<String, String>> getFieldMappings() {
        return Collections.unmodifiableMap(fieldMappings);
    }

    public Map<String, Map<String, String>> getMethodMappings() {
        return Collections.unmodifiableMap(methodMappings);
    }

    public void clear() {
        classMappings.clear();
        fieldMappings.clear();
        methodMappings.clear();
        methodDescriptorMappings.clear();
    }
}