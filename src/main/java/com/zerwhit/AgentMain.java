package com.zerwhit;

import com.zerwhit.core.*;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class AgentMain {
    private static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation inst) {
        initAgent(inst, agentArgs);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        initAgent(inst, agentArgs);
    }

    private static void initAgent(Instrumentation inst, String agentArgs) {
        instrumentation = inst;
        try {
            injectHooksIntoMinecraftClassLoader();
            inst.addTransformer(new ClassTransformer(), true);
            retransformLoadedClasses();
        } catch (Exception e) {
            System.err.println("Failed to initialize agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void injectHooksIntoMinecraftClassLoader() {
        try {
            ClassLoader mcLoader = findMinecraftClassLoader();
            if (mcLoader == null) {
                System.err.println("Could not find Minecraft class loader!");
                return;
            }
            registerClassToLoader(mcLoader, Hooks.class);
            registerClassToLoader(mcLoader, Control.class);
            registerClassToLoader(mcLoader, Meta.class);
        } catch (Exception e) {
            System.err.println("Failed to inject Hooks class: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ClassLoader findMinecraftClassLoader() {
        ClassLoader[] candidates = {
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader(),
                findLaunchClassLoader()
        };

        for (ClassLoader loader : candidates) {
            if (loader != null && loader.getClass().getName().contains("LaunchClassLoader")) {
                return loader;
            }
        }
        return null;
    }

    private static ClassLoader findLaunchClassLoader() {
        try {
            Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
            for (Class<?> clazz : allClasses) {
                ClassLoader loader = clazz.getClassLoader();
                if (loader != null && loader.getClass().getName().contains("LaunchClassLoader")) {
                    return loader;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void registerClassToLoader(ClassLoader targetLoader, Class klass) {
        String className = klass.getName();
        byte[] classBytes = getClassBytes(klass);
        try {
            Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                    "defineClass", String.class, byte[].class, int.class, int.class
            );
            defineClassMethod.setAccessible(true);
            defineClassMethod.invoke(targetLoader, className, classBytes, 0, classBytes.length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to define class in target loader", e);
        }
    }

    private static byte[] getClassBytes(Class klass) {
        try (java.io.InputStream is = AgentMain.class.getResourceAsStream('/' + ClassTransformer.getClassPackage(klass) + ".class");
             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {

            if (is == null) {
                throw new RuntimeException("Could not find Hooks.class in JAR");
            }

            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to read Hooks class bytes", e);
        }
    }

    private static void retransformLoadedClasses() {
        Class[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (Class clazz : allLoadedClasses) {
            String className = clazz.getName().replace('.', '/');
            if (ClassTransformer.isMCClass(className) && instrumentation.isModifiableClass(clazz)) {
                try {
                    instrumentation.retransformClasses(clazz);
                } catch (Exception e) {
                    System.err.println("Failed to retransform " + className + ": " + e.getMessage());
                }
            }
        }
    }
}