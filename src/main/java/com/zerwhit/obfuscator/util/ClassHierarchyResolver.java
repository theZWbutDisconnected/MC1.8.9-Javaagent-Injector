package com.zerwhit.obfuscator.util;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ClassHierarchyResolver {
    private static final Map<String, String> SUPER_CLASS_CACHE = new HashMap<>();
    private static final Map<String, String[]> INTERFACES_CACHE = new HashMap<>();
    private static final Set<String> MISSING_CLASSES = new HashSet<>();
    private static ClassLoader customClassLoader = null;

    public static void setClassLoader(ClassLoader classLoader) {
        customClassLoader = classLoader;
    }

    private static InputStream getClassStream(String className) {
        String classPath = className + ".class";
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

        try (InputStream is = getClassStream(className)) {
            if (is == null) {
                if (!MISSING_CLASSES.contains(className) && !className.startsWith("java/") && !className.startsWith("[")) {
                    System.err.println("Warning: Class not found in classpath: " + className);
                    MISSING_CLASSES.add(className);
                }
                SUPER_CLASS_CACHE.put(className, null);
                return null;
            }

            ClassReader classReader = new ClassReader(is);
            String superName = classReader.getSuperName();
            SUPER_CLASS_CACHE.put(className, superName);
            return superName;
        } catch (IOException e) {
            System.err.println("Error reading class: " + className + " - " + e.getMessage());
            SUPER_CLASS_CACHE.put(className, null);
            return null;
        }
    }

    public static String[] getInterfaces(String className) {
        if (INTERFACES_CACHE.containsKey(className)) {
            return INTERFACES_CACHE.get(className);
        }

        try (InputStream is = getClassStream(className)) {
            if (is == null) {
                INTERFACES_CACHE.put(className, new String[0]);
                return new String[0];
            }

            ClassReader classReader = new ClassReader(is);
            String[] interfaces = classReader.getInterfaces();
            INTERFACES_CACHE.put(className, interfaces);
            return interfaces;
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