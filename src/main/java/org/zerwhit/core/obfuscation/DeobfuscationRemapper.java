package org.zerwhit.core.obfuscation;

import LZMA.LzmaInputStream;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.zerwhit.AgentMain;
import org.zerwhit.core.util.SafeLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeobfuscationRemapper extends Remapper {
    private static final SafeLogger LOGGER = SafeLogger.getLogger(DeobfuscationRemapper.class);
    private static final String MAPPING_FILE_PATH = "mapping/deobfuscation_data-1.8.9.lzma";
    
    private BiMap<String, String> classMappings = ImmutableBiMap.of();
    private Map<String, Map<String, String>> rawMethodMappings = Maps.newHashMap();
    private Map<String, Map<String, String>> rawFieldMappings = Maps.newHashMap();
    
    private Map<String, Map<String, String>> methodMappings = Maps.newHashMap();
    private Map<String, Map<String, String>> fieldMappings = Maps.newHashMap();
    
    private Map<String, Map<String, String>> fieldDescriptions = Maps.newHashMap();
    
    private Set<String> negativeCacheMethods = Sets.newHashSet();
    private Set<String> negativeCacheFields = Sets.newHashSet();
    
    private static DeobfuscationRemapper instance;
    
    private static class ClassInfoReader {
        public static String getSuperName(String className) {
            try {
                InputStream classStream = AgentMain.class.getClassLoader().getResourceAsStream(className + ".class");
                if (classStream != null) {
                    ClassReader reader = new ClassReader(classStream);
                    return reader.getSuperName();
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to read class file for: " + className, e);
            }
            return null;
        }
        
        public static String[] getInterfaces(String className) {
            try {
                InputStream classStream = AgentMain.class.getClassLoader().getResourceAsStream(className + ".class");
                if (classStream != null) {
                    ClassReader reader = new ClassReader(classStream);
                    return reader.getInterfaces();
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to read class file for: " + className, e);
            }
            return new String[0];
        }
        
        public static String getFieldType(String className, String fieldName) {
            try {
                InputStream classStream = AgentMain.class.getClassLoader().getResourceAsStream(className + ".class");
                if (classStream != null) {
                    ClassReader reader = new ClassReader(classStream);
                    FieldTypeVisitor visitor = new FieldTypeVisitor(fieldName);
                    reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    return visitor.getFieldType();
                }
            } catch (IOException e) {
                LOGGER.debug("Failed to read class file for field: " + fieldName + " in " + className, e);
            }
            return null;
        }
    }
    
    private static class FieldTypeVisitor extends ClassVisitor {
        private final String targetFieldName;
        private String fieldType;
        
        public FieldTypeVisitor(String targetFieldName) {
            super(Opcodes.ASM9);
            this.targetFieldName = targetFieldName;
        }
        
        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (name.equals(targetFieldName)) {
                fieldType = descriptor;
            }
            return null;
        }
        
        public String getFieldType() {
            return fieldType;
        }
    }
    
    private DeobfuscationRemapper() {
        loadMappingData();
    }
    
    public static DeobfuscationRemapper getInstance() {
        if (instance == null) {
            instance = new DeobfuscationRemapper();
        }
        return instance;
    }
    
    private void loadMappingData() {
        try {
            InputStream resourceStream = AgentMain.class.getClassLoader().getResourceAsStream(MAPPING_FILE_PATH);
            if (resourceStream == null) {
                LOGGER.error("Could not find deobfuscation mapping file: {}", MAPPING_FILE_PATH);
                return;
            }
            
            byte[] decompressedData = decompressLZMA(resourceStream);
            parseMappingData(decompressedData);
            
            int totalMethods = rawMethodMappings.values().stream().mapToInt(Map::size).sum();
            int totalFields = rawFieldMappings.values().stream().mapToInt(Map::size).sum();
            LOGGER.info("Loaded deobfuscation mapping data: {} classes, {} methods, {} fields", 
                       classMappings.size(), totalMethods, totalFields);
        } catch (Exception e) {
            LOGGER.error("Failed to load deobfuscation mapping data", e);
        }
    }
    
    private byte[] decompressLZMA(InputStream compressedStream) throws IOException {
        try (LzmaInputStream lzmaInputStream = new LzmaInputStream(compressedStream);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = lzmaInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
    }
    
    private void parseMappingData(byte[] data) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8))) {
            String line;
            ImmutableBiMap.Builder<String, String> classBuilder = ImmutableBiMap.builder();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                Splitter splitter = Splitter.on(CharMatcher.anyOf(": ")).omitEmptyStrings().trimResults();
                List<String> parts = splitter.splitToList(line);
                
                if (parts.size() < 2) continue;
                
                String type = parts.get(0);
                if ("CL".equals(type)) {
                    parseClassMapping(classBuilder, parts);
                } else if ("MD".equals(type)) {
                    parseMethodMapping(parts);
                } else if ("FD".equals(type)) {
                    parseFieldMapping(parts);
                }
            }
            
            classMappings = classBuilder.build();
            methodMappings = Maps.newHashMapWithExpectedSize(rawMethodMappings.size());
            fieldMappings = Maps.newHashMapWithExpectedSize(rawFieldMappings.size());
            
        } catch (IOException e) {
            LOGGER.error("Failed to parse mapping data", e);
        }
    }
    
    private void parseClassMapping(ImmutableBiMap.Builder<String, String> builder, List<String> parts) {
        if (parts.size() >= 3) {
            String obfuscatedName = parts.get(1);
            String deobfuscatedName = parts.get(2);
            builder.put(obfuscatedName, deobfuscatedName);
        }
    }
    
    private void parseMethodMapping(List<String> parts) {
        if (parts.size() >= 4) {
            String obfuscatedMethod = parts.get(1);
            String signature = parts.get(2);
            String deobfuscatedMethod = parts.get(3);
            
            int lastSlash = obfuscatedMethod.lastIndexOf('/');
            if (lastSlash > 0) {
                String className = obfuscatedMethod.substring(0, lastSlash);
                String methodName = obfuscatedMethod.substring(lastSlash + 1);
                
                if (!rawMethodMappings.containsKey(className)) {
                    rawMethodMappings.put(className, Maps.newHashMap());
                }
                rawMethodMappings.get(className).put(methodName + signature, deobfuscatedMethod);
            }
        }
    }
    
    private void parseFieldMapping(List<String> parts) {
        if (parts.size() >= 3) {
            String obfuscatedField = parts.get(1);
            String deobfuscatedField = parts.get(2);
            
            int lastSlash = obfuscatedField.lastIndexOf('/');
            if (lastSlash > 0) {
                String className = obfuscatedField.substring(0, lastSlash);
                String fieldName = obfuscatedField.substring(lastSlash + 1);
                
                if (!rawFieldMappings.containsKey(className)) {
                    rawFieldMappings.put(className, Maps.newHashMap());
                }
                rawFieldMappings.get(className).put(fieldName + ":null", deobfuscatedField);
            }
        }
    }

    @Override
    public String map(String typeName) {
        if (classMappings == null || classMappings.isEmpty()) {
            return typeName;
        }
        if (classMappings.containsKey(typeName)) {
            return classMappings.get(typeName);
        }
        int dollarIdx = typeName.lastIndexOf('$');
        if (dollarIdx > -1) {
            return map(typeName.substring(0, dollarIdx)) + "$" + typeName.substring(dollarIdx + 1);
        }
        return typeName;
    }
    
    public String unmap(String typeName) {
        if (classMappings == null || classMappings.isEmpty()) {
            return typeName;
        }
        if (classMappings.containsValue(typeName)) {
            return classMappings.inverse().get(typeName);
        }
        int dollarIdx = typeName.lastIndexOf('$');
        if (dollarIdx > -1) {
            return unmap(typeName.substring(0, dollarIdx)) + "$" + typeName.substring(dollarIdx + 1);
        }
        return typeName;
    }
    
    public String mapMethodName(String owner, String name, String desc) {
        if (classMappings == null || classMappings.isEmpty()) {
            return name;
        }
        Map<String, String> methodMap = getMethodMap(owner);
        String methodDescriptor = name + desc;
        return methodMap != null && methodMap.containsKey(methodDescriptor) ? methodMap.get(methodDescriptor) : name;
    }
    
    public String mapFieldName(String owner, String name, String desc) {
        if (classMappings == null || classMappings.isEmpty()) {
            return name;
        }
        Map<String, String> fieldMap = getFieldMap(owner);
        return fieldMap != null && fieldMap.containsKey(name + ":" + desc) ? fieldMap.get(name + ":" + desc) : name;
    }
    
    private Map<String, String> getFieldMap(String className) {
        if (!fieldMappings.containsKey(className)) {
            mergeSuperMaps(className);
        }
        return fieldMappings.get(className);
    }
    
    private Map<String, String> getMethodMap(String className) {
        if (!methodMappings.containsKey(className) && !negativeCacheMethods.contains(className)) {
            findAndMergeSuperMaps(className);
            if (!methodMappings.containsKey(className)) {
                negativeCacheMethods.add(className);
            }
        }
        return methodMappings.get(className);
    }
    
    private String getFieldType(String owner, String name) {
        if (fieldDescriptions.containsKey(owner)) {
            Map<String, String> classMap = fieldDescriptions.get(owner);
            if (classMap.containsKey(name)) {
                return classMap.get(name);
            }
        }
        
        synchronized (fieldDescriptions) {
            try {
                String fieldType = ClassInfoReader.getFieldType(owner, name);
                if (fieldType != null) {
                    if (!fieldDescriptions.containsKey(owner)) {
                        fieldDescriptions.put(owner, Maps.newHashMap());
                    }
                    fieldDescriptions.get(owner).put(name, fieldType);
                    return fieldType;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get field type for {} in {}", name, owner, e);
            }
            return null;
        }
    }
    
    private void findAndMergeSuperMaps(String name) {
        try {
            String superName = ClassInfoReader.getSuperName(name);
            String[] interfaces = ClassInfoReader.getInterfaces(name);
            mergeSuperMaps(name, superName, interfaces);
        } catch (Exception e) {
            LOGGER.error("Failed to find and merge super maps for class: {}", name, e);
        }
    }
    
    public void mergeSuperMaps(String name, String superName, String[] interfaces) {
        if (classMappings == null || classMappings.isEmpty()) {
            return;
        }
        
        // Skip Object
        if (superName == null || superName.equals("java/lang/Object")) {
            return;
        }
        
        List<String> allParents = ImmutableList.<String>builder()
                .add(superName)
                .addAll(Arrays.asList(interfaces))
                .build();
        
        // generate maps for all parent objects
        for (String parentThing : allParents) {
            if (!methodMappings.containsKey(parentThing)) {
                findAndMergeSuperMaps(parentThing);
            }
        }
        
        Map<String, String> methodMap = Maps.newHashMap();
        Map<String, String> fieldMap = Maps.newHashMap();
        
        for (String parentThing : allParents) {
            if (methodMappings.containsKey(parentThing)) {
                methodMap.putAll(methodMappings.get(parentThing));
            }
            if (fieldMappings.containsKey(parentThing)) {
                fieldMap.putAll(fieldMappings.get(parentThing));
            }
        }
        
        if (rawMethodMappings.containsKey(name)) {
            methodMap.putAll(rawMethodMappings.get(name));
        }
        if (rawFieldMappings.containsKey(name)) {
            fieldMap.putAll(rawFieldMappings.get(name));
        }
        
        methodMappings.put(name, ImmutableMap.copyOf(methodMap));
        fieldMappings.put(name, ImmutableMap.copyOf(fieldMap));
    }
    
    public void mergeSuperMaps(String className) {
        findAndMergeSuperMaps(className);
    }

    public String getStaticFieldType(String oldType, String oldName, String newType, String newName)
    {
        String fType = getFieldType(oldType, oldName);
        if (oldType.equals(newType))
        {
            return fType;
        }
        Map<String,String> newClassMap = fieldDescriptions.get(newType);
        if (newClassMap == null)
        {
            newClassMap = Maps.newHashMap();
            fieldDescriptions.put(newType, newClassMap);
        }
        newClassMap.put(newName, fType);
        return fType;
    }
    
    public boolean hasMappingData() {
        return !classMappings.isEmpty() || !rawMethodMappings.isEmpty() || !rawFieldMappings.isEmpty();
    }
    
    public String getDeobfuscatedClassName(String obfuscatedName) {
        return map(obfuscatedName);
    }
    
    public String getDeobfuscatedMethodName(String className, String obfuscatedMethod) {
        return mapMethodName(className, obfuscatedMethod, "");
    }
    
    public String getDeobfuscatedFieldName(String className, String obfuscatedField) {
        return mapFieldName(className, obfuscatedField, "null");
    }
    
    public String getObfuscatedClassName(String deobfuscatedName) {
        return unmap(deobfuscatedName);
    }
    
    public String getObfuscatedMethodName(String className, String deobfuscatedMethod) {
        Map<String, String> methodMap = getMethodMap(className);
        if (methodMap != null) {
            for (Map.Entry<String, String> entry : methodMap.entrySet()) {
                if (entry.getValue().equals(deobfuscatedMethod)) {
                    return entry.getKey().substring(0, entry.getKey().indexOf('('));
                }
            }
        }
        return deobfuscatedMethod;
    }
    
    public String getObfuscatedFieldName(String className, String deobfuscatedField) {
        Map<String, String> fieldMap = getFieldMap(className);
        if (fieldMap != null) {
            for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                if (entry.getValue().equals(deobfuscatedField)) {
                    return entry.getKey().substring(0, entry.getKey().indexOf(':'));
                }
            }
        }
        return deobfuscatedField;
    }
}