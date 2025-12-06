package com.zerwhit.obfuscator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.zerwhit.obfuscator.parser.CsvParser;
import com.zerwhit.obfuscator.parser.TsrgParser;
import com.zerwhit.obfuscator.util.ClassHierarchyResolver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BytecodeObfuscator {
    private static final Logger logger = LogManager.getLogger(BytecodeObfuscator.class);
    
    private static CsvParser csvParser;
    private TsrgParser tsrgParser;
    private Set<String> excludedClasses = new HashSet<>();
    private Set<String> excludedMethods = new HashSet<>();

    private static String minecraftJarPath = "minecraft.jar";

    private static final List<String> TARGET_PACKAGES = Arrays.asList(
            "com/zerwhit/core/"
    );

    private static final List<String> MINECRAFT_PACKAGES = Arrays.asList(
            "net/minecraft", "com/mojang", "badlion"
    );

    public BytecodeObfuscator() {
        this.tsrgParser = new TsrgParser();
        initializeExclusions();
        setupMinecraftClassLoader();
    }

    public static void setMinecraftJarPath(String jarPath) {
        minecraftJarPath = jarPath;
    }

    private void initializeExclusions() {
        excludedMethods.addAll(Arrays.asList(
                "main", "premain", "agentmain"
        ));
    }

    private void setupMinecraftClassLoader() {
        try {
            File jarFile = new File(minecraftJarPath);
            if (!jarFile.exists()) {
                logger.warn("Minecraft JAR not found at: {}", minecraftJarPath);
                logger.warn("Trying to find minecraft.jar in current directory...");

                File currentDir = new File(".");
                File[] jars = currentDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar") &&
                        (name.toLowerCase().contains("minecraft") || name.toLowerCase().contains("client")));

                if (jars != null && jars.length > 0) {
                    jarFile = jars[0];
                    logger.info("Found Minecraft JAR: {}", jarFile.getName());
                } else {
                    logger.warn("No Minecraft JAR found. Class hierarchy resolution may not work properly.");
                    return;
                }
            }

            URL jarUrl = jarFile.toURI().toURL();
            URLClassLoader mcClassLoader = new URLClassLoader(new URL[]{jarUrl},
                    BytecodeObfuscator.class.getClassLoader());

            ClassHierarchyResolver.setClassLoader(mcClassLoader);

            logger.info("Successfully loaded Minecraft JAR: {}", jarFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to setup Minecraft class loader:", e);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            logger.info("Usage: BytecodeObfuscator <classes-directory> <tsrg-file> <fields-csv> <methods-csv> [minecraft-jar]");
            logger.info("Example: BytecodeObfuscator build/classes/java/main mappings/srg-mcp-1.12.2.tsrg fields.csv methods.csv");
            return;
        }

        String classesDir = args[0];
        String tsrgFile = args[1];
        String fieldsCsv = args[2];
        String methodsCsv = args[3];

        if (args.length > 4) {
            setMinecraftJarPath(args[4]);
        }

        csvParser = new CsvParser(fieldsCsv, methodsCsv);
        BytecodeObfuscator obfuscator = new BytecodeObfuscator();
        try {
            obfuscator.obfuscate(classesDir, tsrgFile);
        } catch (IOException e) {
            logger.error("Obfuscation failed: {}", e.getMessage(), e);
        }
    }

    public void obfuscate(String classesDir, String tsrgFile) throws IOException {
        logger.info("Starting bytecode obfuscation...");
        logger.info("Classes directory: {}", classesDir);
        logger.info("TSRG file: {}", tsrgFile);

        ClassHierarchyResolver.clearCache();

        tsrgParser.parseTsrgFile(tsrgFile);
        logger.info("Loaded {} class mappings", tsrgParser.getClassMappings().size());
        logger.info("Loaded {} field mappings", tsrgParser.getFieldMappings().values().stream().mapToInt(Map::size).sum());
        logger.info("Loaded {} method mappings", tsrgParser.getMethodMappings().values().stream().mapToInt(Map::size).sum());

        Path classesPath = Paths.get(classesDir);
        if (!Files.exists(classesPath)) {
            throw new IOException("Classes directory does not exist: " + classesDir);
        }

        Path basePath = classesPath;
        Files.walk(classesPath)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> processClassFile(path, basePath));

        logger.info("Bytecode obfuscation completed!");
    }

    private void processClassFile(Path classFile, Path basePath) {
        try {
            byte[] classBytes = Files.readAllBytes(classFile);
            String className = getClassNameFromPath(classFile, basePath);

            if (!isValidClassFile(classBytes)) {
                logger.error("Invalid class file: {}", className);
                return;
            }

            if (shouldObfuscateClass(className)) {
                byte[] obfuscatedBytes = obfuscateClass(classBytes, className);
                Files.write(classFile, obfuscatedBytes);
            } else {
                logger.debug("Skipped: {}", className);
            }
        } catch (Exception e) {
            logger.error("Failed to process class file: {}", classFile, e);
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
            logger.debug("Excluded class: {}", className);
            return false;
        }
        for (String targetPkg : TARGET_PACKAGES) {
            if (className.startsWith(targetPkg)) {
                logger.debug("Target class for obfuscation: {}", className);
                return true;
            }
        }
        return false;
    }

    private byte[] obfuscateClass(byte[] classBytes, String className) {
        try {
            ClassReader classReader = new ClassReader(classBytes);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            ClassVisitor classVisitor = new ObfuscatingClassVisitor(classWriter, className, tsrgParser, csvParser);
            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);

            return classWriter.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to obfuscate class: {}", className, e);
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