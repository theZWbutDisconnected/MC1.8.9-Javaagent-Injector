package org.zerwhit.core.obfuscation;

import org.zerwhit.core.util.SafeLogger;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ClassHierarchyResolver {
    private static final SafeLogger logger = SafeLogger.getLogger(ClassHierarchyResolver.class);
    
    private static final Map<String, String> SUPER_CLASS_CACHE = new HashMap<>();
    private static final Map<String, String[]> INTERFACES_CACHE = new HashMap<>();
    private static final Set<String> MISSING_CLASSES = new HashSet<>();
    private static ClassLoader customClassLoader = null;

    public static void setClassLoader(ClassLoader classLoader) {
        customClassLoader = classLoader;
    }

    private static InputStream getClassStream(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        
        // Validate class name format - should not contain special characters that might indicate invalid input
        if (className.contains(" ") || className.contains("\n") || className.contains("\r")) {
            logger.warn("Invalid class name format: {}", className);
            return null;
        }
        
        // Convert class name to resource path format
        String classPath = className.replace('.', '/') + ".class";
        
        if (customClassLoader != null) {
            InputStream is = customClassLoader.getResourceAsStream(classPath);
            if (is != null) {
                return is;
            }
        }
        return ClassHierarchyResolver.class.getClassLoader().getResourceAsStream(classPath);
    }

    public static String getSuperClass(String className) {
        if (SUPER_CLASS_CACHE.containsKey(className)) {
            return SUPER_CLASS_CACHE.get(className);
        }

        // Validate class name - if it's too short or has invalid format, skip it
        if (className == null || className.length() < 3 || !className.matches("[a-zA-Z0-9_/$]+")) {
            logger.warn("Invalid class name format for super class resolution: {}", className);
            SUPER_CLASS_CACHE.put(className, null);
            return null;
        }

        try (InputStream is = getClassStream(className)) {
            if (is == null) {
                if (!MISSING_CLASSES.contains(className) && !className.startsWith("java/") && !className.startsWith("[")) {
                    logger.warn("Class not found in classpath: {}", className);
                    MISSING_CLASSES.add(className);
                }
                SUPER_CLASS_CACHE.put(className, null);
                return null;
            }

            logger.info("Reading class: {}", className);

            try {
                ClassReader classReader = new ClassReader(is);
                String superName = classReader.getSuperName();
                if (superName != null && superName.isEmpty()) {
                    superName = null;
                }
                SUPER_CLASS_CACHE.put(className, superName);
                return superName;
            } catch (IllegalArgumentException e) {
                logger.error("Invalid class format for class: {}", className, e);
                SUPER_CLASS_CACHE.put(className, null);
                return null;
            }
        } catch (IOException e) {
            logger.error("Error reading class: {}", className, e);
            SUPER_CLASS_CACHE.put(className, null);
            return null;
        }
    }

    public static String[] getInterfaces(String className) {
        if (INTERFACES_CACHE.containsKey(className)) {
            return INTERFACES_CACHE.get(className);
        }

        // Validate class name - if it's too short or has invalid format, skip it
        if (className == null || className.length() < 3 || !className.matches("[a-zA-Z0-9_/$]+")) {
            logger.warn("Invalid class name format for interface resolution: {}", className);
            INTERFACES_CACHE.put(className, new String[0]);
            return new String[0];
        }

        try (InputStream is = getClassStream(className)) {
            if (is == null) {
                INTERFACES_CACHE.put(className, new String[0]);
                return new String[0];
            }

            try {
                ClassReader classReader = new ClassReader(is);
                String[] interfaces = classReader.getInterfaces();
                if (interfaces == null) {
                    interfaces = new String[0];
                }
                INTERFACES_CACHE.put(className, interfaces);
                return interfaces;
            } catch (IllegalArgumentException e) {
                logger.error("Invalid class format for interfaces in class: {}", className, e);
                INTERFACES_CACHE.put(className, new String[0]);
                return new String[0];
            }
        } catch (IOException e) {
            INTERFACES_CACHE.put(className, new String[0]);
            return new String[0];
        }
    }

    public static List<String> getClassHierarchy(String className) {
        List<String> hierarchy = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        getClassHierarchyRecursive(className, hierarchy, visited);

        return hierarchy;
    }

    private static void getClassHierarchyRecursive(String className, List<String> hierarchy, Set<String> visited) {
        if (className == null || className.equals("java/lang/Object") || visited.contains(className)) {
            return;
        }

        visited.add(className);
        hierarchy.add(className);

        String superClass = getSuperClass(className);
        if (superClass != null) {
            getClassHierarchyRecursive(superClass, hierarchy, visited);
        }

        String[] interfaces = getInterfaces(className);
        for (String iface : interfaces) {
            getClassHierarchyRecursive(iface, hierarchy, visited);
        }
    }

    public static void clearCache() {
        SUPER_CLASS_CACHE.clear();
        INTERFACES_CACHE.clear();
        MISSING_CLASSES.clear();
    }
}