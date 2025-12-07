package org.zerwhit;

import org.zerwhit.core.Hooks;
import org.tzd.agent.nativeapi.AgentNative;
import org.zerwhit.core.ClassTransformer;
import org.zerwhit.core.util.SafeLogger;
import org.zerwhit.core.obfuscation.FMLDeobfuscatingRemapper;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AgentMain {
    private static final SafeLogger logger = SafeLogger.getLogger(AgentMain.class);
    public static List<ClassLoader> mcLoader = new ArrayList<>();
    public static void tzdAgentMain(AgentNative.AgentHandle handle, String[] agentArgs) throws Exception {
        logger.debug("tzdAgentMain called with handle: {}", handle);
        initAgent(handle, agentArgs);
    }

    private static void injectHooksIntoMinecraftClassLoader() {
        try {
            ClassLoader mcLoader = findMinecraftClassLoader();
            if (mcLoader == null) {
                logger.error("Could not find Minecraft class loader!");
                return;
            }
            String agentJarPath = AgentMain.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
            if (mcLoader.getClass().getName().equals("net.minecraft.launchwrapper.LaunchClassLoader") || mcLoader.getClass().getName().equals("jdk.internal.loader.ClassLoaders")) {
                Method addURLMethod = mcLoader.getClass().getDeclaredMethod("addURL", URL.class);
                addURLMethod.setAccessible(true);
                addURLMethod.invoke(mcLoader, new File(agentJarPath).toURI().toURL());
            }
//            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(agentJarPath));
//            instrumentation.appendToSystemClassLoaderSearch(new JarFile(agentJarPath));
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
            if (loader != null && loader.getClass().getName().contains("LaunchClassLoader") || loader.getClass().getName().equals("ClassLoaders")) {
                return loader;
            }
        }
        return null;
    }

    private static ClassLoader findLaunchClassLoader() {
        try {
            for (Class<?> clazz : AgentNative.getAllLoadedClasses()) {
                ClassLoader loader = clazz.getClassLoader();
                if (loader != null && loader.getClass().getName().contains("LaunchClassLoader")) {
                    return loader;
                }
            }
        } catch (Exception e) {
            logger.error("Error while finding LaunchClassLoader: {}", e.getMessage());
        }
        return null;
    }
    
    private static void initAgent(AgentNative.AgentHandle handle, String[] agentArgs) {
        logger.info("Initializing agent with handle: {}", handle);
        try {
            String dllPath = getDllPath();
            System.load(dllPath);
            logger.info("Loaded TzdAgent.dll from: {}", dllPath);
            logger.info(Hooks.class.getName());
            logger.info(AgentNative.getAllLoadedClasses().toString());
            if (handle != null) {
                injectHooksIntoMinecraftClassLoader();
                FMLDeobfuscatingRemapper.INSTANCE.setupLoadOnly(
                        "/mapping/deobfuscation_data-1.8.9.srg",true
                    );
                AgentNative.addTransformer(handle, new AgentNative.ClassFileTransformerEncapsulation(
                    new ClassTransformer(), true
                ));
                retransformLoadedClasses(handle);
                logger.info("Transformer added successfully");
            } else {
                throw new IllegalArgumentException("Agent handle is null");
            }
            logger.info("Agent initialization completed successfully");
        } catch (Exception e) {
            logger.error("Error during agent initialization: {}", e.getMessage());
            logger.error("Agent initialization error details:", e);
        }
    }

    private static void retransformLoadedClasses(AgentNative.AgentHandle handle) {
        for (Class<?> clazz : AgentNative.getAllLoadedClasses()) {
            String className = clazz.getName().replace('.', '/');
            if (ClassTransformer.isMCClass(className) && AgentNative.isModifiableClass(clazz)) {
                try {
                    AgentNative.retransformClasses(handle, clazz);
                } catch (Exception e) {
                    logger.warn("Failed to retransform {}", className);
                }
            }
        }
    }

    private static String getDllPath() {
        java.net.URL dllUrl = AgentMain.class.getClassLoader().getResource("TzdAgent.dll");
        if (dllUrl != null) {
            try {
                java.io.InputStream inputStream = AgentMain.class.getClassLoader().getResourceAsStream("TzdAgent.dll");
                if (inputStream != null) {
                    java.io.File tempFile = java.io.File.createTempFile("TzdAgent", ".dll");
                    tempFile.deleteOnExit();
                    
                    try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    
                    return tempFile.getAbsolutePath();
                }
            } catch (Exception e) {
                logger.error("Error extracting DLL: {}", e.getMessage());
            }
        }

        return "TzdAgent.dll";
    }
}