package com.zerwhit.obfuscator;

import com.zerwhit.obfuscator.parser.CsvParser;
import com.zerwhit.obfuscator.parser.TsrgParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BytecodeObfuscator {
    private static CsvParser csvParser;
    private TsrgParser tsrgParser;
    private Set<String> excludedClasses = new HashSet<>();
    private Set<String> excludedMethods = new HashSet<>();

    private static final List<String> TARGET_PACKAGES = Arrays.asList(
            "com/zerwhit/core/"
    );
    
    private static final List<String> MINECRAFT_PACKAGES = Arrays.asList(
            "net/minecraft", "com/mojang", "badlion"
    );

    public BytecodeObfuscator() {
        this.tsrgParser = new TsrgParser();
        initializeExclusions();
    }

    private void initializeExclusions() {
        excludedMethods.addAll(Arrays.asList(
                "main", "premain", "agentmain"
        ));
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: BytecodeObfuscator <classes-directory> <tsrg-file>");
            System.out.println("Example: BytecodeObfuscator build/classes/java/main mappings/srg-mcp-1.12.2.tsrg");
            return;
        }

        String classesDir = args[0];
        String tsrgFile = args[1];
        csvParser = new CsvParser(args[2], args[3]);

        BytecodeObfuscator obfuscator = new BytecodeObfuscator();
        try {
            obfuscator.obfuscate(classesDir, tsrgFile);
        } catch (IOException e) {
            System.err.println("Obfuscation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void obfuscate(String classesDir, String tsrgFile) throws IOException {
        System.out.println("Starting bytecode obfuscation...");
        System.out.println("Classes directory: " + classesDir);
        System.out.println("TSRG file: " + tsrgFile);

        tsrgParser.parseTsrgFile(tsrgFile);
        System.out.println("Loaded " + tsrgParser.getClassMappings().size() + " class mappings");
        System.out.println("Loaded " + tsrgParser.getFieldMappings().values().stream().mapToInt(Map::size).sum() + " field mappings");
        System.out.println("Loaded " + tsrgParser.getMethodMappings().values().stream().mapToInt(Map::size).sum() + " method mappings");

        Path classesPath = Paths.get(classesDir);
        if (!Files.exists(classesPath)) {
            throw new IOException("Classes directory does not exist: " + classesDir);
        }

        Path basePath = classesPath;
        Files.walk(classesPath)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> processClassFile(path, basePath));

        System.out.println("Bytecode obfuscation completed!");
    }

    private void processClassFile(Path classFile, Path basePath) {
        try {
            byte[] classBytes = Files.readAllBytes(classFile);
            String className = getClassNameFromPath(classFile, basePath);

            if (!isValidClassFile(classBytes)) {
                System.err.println("Invalid class file: " + className);
                return;
            }

            if (shouldObfuscateClass(className)) {
                byte[] obfuscatedBytes = obfuscateClass(classBytes, className);
                Files.write(classFile, obfuscatedBytes);
            } else {
                System.out.println("Skipped: " + className);
            }
        } catch (Exception e) {
            System.err.println("Failed to process class file: " + classFile);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isValidClassFile(byte[] classBytes) {
        return classBytes.length > 4 &&
                classBytes[0] == (byte)0xCA &&
                classBytes[1] == (byte)0xFE &&
                classBytes[2] == (byte)0xBA &&
                classBytes[3] == (byte)0xBE;
    }

    private boolean shouldObfuscateClass(String className) {
        if (excludedClasses.contains(className)) {
            System.out.println("Excluded class: " + className);
            return false;
        }
        for (String targetPkg : TARGET_PACKAGES) {
            if (className.startsWith(targetPkg)) {
                System.out.println("Target class for obfuscation: " + className);
                return true;
            }
        }
        return false;
    }

    private byte[] obfuscateClass(byte[] classBytes, String className) {
        try {
            ClassReader classReader = new ClassReader(classBytes);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            // 检查类是否继承自Minecraft类或实现Minecraft接口
            ClassVisitor classVisitor = new ObfuscatingClassVisitor(classWriter, className, tsrgParser, csvParser);
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

            return classWriter.toByteArray();
        } catch (Exception e) {
            System.err.println("Failed to obfuscate class: " + className);
            e.printStackTrace();
            return classBytes;
        }
    }
    
    private boolean isMinecraftClass(String className) {
        for (String mcPkg : MINECRAFT_PACKAGES) {
            if (className.startsWith(mcPkg)) {
                return true;
            }
        }
        return false;
    }

    private String getClassNameFromPath(Path classFile, Path basePath) {
        try {
            Path relativePath = basePath.relativize(classFile);
            String className = relativePath.toString()
                    .replace(File.separatorChar, '/')
                    .replace(".class", "");
            return className;
        } catch (IllegalArgumentException e) {
            String fullPath = classFile.toString().replace('\\', '/');
            String classesDir = basePath.toString().replace('\\', '/');

            if (fullPath.startsWith(classesDir)) {
                String className = fullPath.substring(classesDir.length() + 1)
                        .replace(".class", "");
                return className;
            }

            String fileName = classFile.getFileName().toString();
            return fileName.replace(".class", "");
        }
    }

    public TsrgParser getTsrgParser() {
        return tsrgParser;
    }

    public Set<String> getExcludedClasses() {
        return Collections.unmodifiableSet(excludedClasses);
    }

    public Set<String> getExcludedMethods() {
        return Collections.unmodifiableSet(excludedMethods);
    }
}