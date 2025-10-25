package com.zerwhit;

import com.zerwhit.core.Control;
import com.zerwhit.core.Hooks;
import com.zerwhit.core.Meta;
import com.zerwhit.core.jni.GraphicLib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;

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
        }
    }

    private static void injectHooksIntoMinecraftClassLoader() {
        try {
            ClassLoader mcLoader = findMinecraftClassLoader();
            if (mcLoader == null) {
                System.err.println("Could not find Minecraft class loader!");
                return;
            }
            String agentJarPath = AgentMain.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
            if (mcLoader.getClass().getName().equals("net.minecraft.launchwrapper.LaunchClassLoader")) {
                Method addURLMethod = mcLoader.getClass().getDeclaredMethod("addURL", URL.class);
                addURLMethod.setAccessible(true);
                addURLMethod.invoke(mcLoader, new File(agentJarPath).toURI().toURL());
            }
            registerClassesToLoader(mcLoader, GraphicLib.class, Hooks.class, Control.class, Meta.class);
        } catch (Exception e) {
            System.err.println("Failed to inject Hooks class: " + e.getMessage());
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
            for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
                ClassLoader loader = clazz.getClassLoader();
                if (loader != null && loader.getClass().getName().contains("LaunchClassLoader")) {
                    return loader;
                }
            }
        } catch (Exception e) {
            // Ignore exception, return null
        }
        return null;
    }

    private static void registerClassesToLoader(ClassLoader targetLoader, Class<?>... classes) {
        for (Class<?> klass : classes) {
            registerClassToLoader(targetLoader, klass);
        }
    }

    private static void registerClassToLoader(ClassLoader targetLoader, Class<?> klass) {
        try {
            byte[] classBytes = getClassBytes(klass);
            Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                    "defineClass", String.class, byte[].class, int.class, int.class);
            defineClassMethod.setAccessible(true);
            defineClassMethod.invoke(targetLoader, klass.getName(), classBytes, 0, classBytes.length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to define class in target loader", e);
        }
    }

    private static byte[] getClassBytes(Class<?> klass) {
        try (InputStream is = AgentMain.class
                .getResourceAsStream('/' + ClassTransformer.getClassPackage(klass) + ".class");
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            if (is == null) {
                throw new RuntimeException("Could not find " + klass.getSimpleName() + ".class in JAR");
            }

            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read class bytes", e);
        }
    }

    private static void retransformLoadedClasses() {
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            String className = clazz.getName().replace('.', '/');
            if (ClassTransformer.isMCClass(className) && instrumentation.isModifiableClass(clazz)) {
                try {
                    instrumentation.retransformClasses(clazz);
                } catch (Exception e) {
                    System.err.println("Failed to retransform " + className);
                }
            }
        }
    }
}