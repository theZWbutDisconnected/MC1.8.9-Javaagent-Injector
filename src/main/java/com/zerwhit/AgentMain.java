package com.zerwhit;

import net.minecraft.client.Minecraft;
import org.tzd.agent.nativeapi.AgentNative;
import com.zerwhit.core.ClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class AgentMain {
    private static final Logger logger = LogManager.getLogger(AgentMain.class);
    public static List<ClassLoader> mcLoader = new ArrayList<>();
    public static void tzdAgentMain(AgentNative.AgentHandle handle, String[] agentArgs) throws Exception {
        logger.debug("tzdAgentMain called with handle: {}", handle);
        initAgent(handle, agentArgs);
    }

    private static void injectHooksIntoMinecraftClassLoader() {
        try {
            findLaunchClassLoader();
            if (mcLoader == null || mcLoader.isEmpty()) {
                throw new RuntimeException("Could not find Minecraft class loader!");
            }
            for (ClassLoader loader : mcLoader) {
                String agentJarPath = AgentMain.class.getProtectionDomain()
                        .getCodeSource().getLocation().getPath();
                Method addURLMethod = loader.getClass().getDeclaredMethod("addURL", URL.class);
                addURLMethod.setAccessible(true);
                addURLMethod.invoke(loader, new File(agentJarPath).toURI().toURL());
            }
//            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(agentJarPath));
//            instrumentation.appendToSystemClassLoaderSearch(new JarFile(agentJarPath));
        } catch (Exception e) {
            logger.error("Failed to inject Hooks class: {}", e.getMessage());
        }
    }

    private static void findLaunchClassLoader() {
        for (Object clazzLoader : AgentNative.getInstances(ClassLoader.class)) {
            ClassLoader loader = (ClassLoader) clazzLoader;
            if (loader != null) {
                mcLoader.add(loader);
            }
        }
    }
    
    private static void initAgent(AgentNative.AgentHandle handle, String[] agentArgs) {
        logger.info("Initializing agent with handle: {}", handle);
        try {
            String dllPath = getDllPath();
            System.load(dllPath);
            logger.info("Loaded TzdAgent.dll from: {}", dllPath);
            if (handle != null) {
                injectHooksIntoMinecraftClassLoader();
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