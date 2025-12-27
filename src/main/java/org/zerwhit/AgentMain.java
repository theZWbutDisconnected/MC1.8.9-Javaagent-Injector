package org.zerwhit;

import org.zerwhit.core.Hooks;
import org.tzd.agent.nativeapi.AgentNative;
import org.zerwhit.core.ClassTransformer;
import org.zerwhit.core.util.SafeLogger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public final class AgentMain {
    private static final SafeLogger logger = SafeLogger.getLogger(AgentMain.class);
    
    private static ClassLoader mcLoader;
    private static ClassTransformer classTransformer;
    
    private AgentMain() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    public static void tzdAgentMain(AgentNative.AgentHandle handle, String[] agentArgs) throws Exception {
        initAgent(handle, agentArgs);
    }
    
    private static void injectHooksIntoMinecraftClassLoader() {
        try {
            mcLoader = findMinecraftClassLoader();
            if (mcLoader == null) {
                logger.error("Could not find Minecraft class loader!");
                return;
            }
            
            String agentJarPath = getAgentJarPath();
            Method addURLMethod = getAddURLMethod();
            addURLToClassLoader(addURLMethod, agentJarPath);
            
        } catch (Exception e) {
            handleInjectionError(e);
        }
    }
    
    private static String getAgentJarPath() {
        return AgentMain.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
    }
    
    private static Method getAddURLMethod() throws NoSuchMethodException {
        Method method = mcLoader.getClass().getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        return method;
    }
    
    private static void addURLToClassLoader(Method addURLMethod, String agentJarPath) throws Exception {
        addURLMethod.invoke(mcLoader, new File(agentJarPath).toURI().toURL());
    }
    
    private static void handleInjectionError(Exception e) {
        System.err.println("Failed to inject Hooks class: " + e.getMessage());
    }
    
    private static ClassLoader findMinecraftClassLoader() {
        ClassLoader minecraftLoader = findMinecraftLaunchClassLoader();
        if (minecraftLoader != null) {
            return minecraftLoader;
        }
        ClassLoader[] candidates = {
                findLaunchClassLoader(),
                Thread.currentThread().getContextClassLoader(),
                ClassLoader.getSystemClassLoader()
        };
        return findValidClassLoader(candidates);
    }
    
    private static ClassLoader findValidClassLoader(ClassLoader[] candidates) {
        for (ClassLoader loader : candidates) {
            if (isMinecraftClassLoader(loader)) {
                return loader;
            }
        }
        return null;
    }
    
    private static boolean isMinecraftClassLoader(ClassLoader loader) {
        if (loader == null) {
            return false;
        }
        String className = loader.getClass().getName();
        return className.equals("net.minecraft.launchwrapper.LaunchClassLoader") ||
               className.contains("ClassLoaders");
    }
    
    private static ClassLoader findMinecraftLaunchClassLoader() {
        try {
            for (Class<?> clazz : AgentNative.getAllLoadedClasses()) {
                ClassLoader loader = clazz.getClassLoader();
                if (loader != null && 
                    "net.minecraft.launchwrapper.LaunchClassLoader".equals(loader.getClass().getName())) {
                    return loader;
                }
            }
        } catch (Exception e) {
            logger.error("Error while finding Minecraft LaunchClassLoader: {}", e.getMessage());
        }
        return null;
    }
    
    private static ClassLoader findLaunchClassLoader() {
        try {
            return searchForLaunchClassLoader();
        } catch (Exception e) {
            logger.error("Error while finding LaunchClassLoader: {}", e.getMessage());
            return null;
        }
    }
    
    private static ClassLoader searchForLaunchClassLoader() {
        for (Class<?> clazz : AgentNative.getAllLoadedClasses()) {
            ClassLoader loader = clazz.getClassLoader();
            if (loader != null && loader.getClass().getName().contains("LaunchClassLoader")) {
                return loader;
            }
        }
        return null;
    }
    
    private static void initAgent(AgentNative.AgentHandle handle, String[] agentArgs) {
        try {
            initializeDll();
            setupTransformer(handle);
            retransformLoadedClasses(handle);
        } catch (Exception e) {
            logger.error("Error during agent initialization: {}", e.getMessage());
        }
    }
    
    private static void initializeDll() throws Exception {
        String dllPath = getDllPath();
        System.load(dllPath);
        logger.info("Loaded TzdAgent.dll from: {}", dllPath);
    }
    
    private static void setupTransformer(AgentNative.AgentHandle handle) throws Exception {
        if (handle == null) {
            throw new IllegalArgumentException("Agent handle is null");
        }
        injectHooksIntoMinecraftClassLoader();
        classTransformer = new ClassTransformer();
        AgentNative.addTransformer(handle, new AgentNative.ClassFileTransformerEncapsulation(
                classTransformer, true
        ));
    }
    
    private static void retransformLoadedClasses(AgentNative.AgentHandle handle) {
        for (Class<?> clazz : AgentNative.getAllLoadedClasses()) {
            if (shouldRetransformClass(clazz)) {
                retransformSingleClass(handle, clazz);
            }
        }
    }
    
    private static boolean shouldRetransformClass(Class<?> clazz) {
        String className = clazz.getName().replace('.', '/');
        return ClassTransformer.isMCClass(className) && AgentNative.isModifiableClass(clazz);
    }
    
    private static void retransformSingleClass(AgentNative.AgentHandle handle, Class<?> clazz) {
        try {
            AgentNative.retransformClasses(handle, clazz);
        } catch (Exception e) {
            String className = clazz.getName().replace('.', '/');
            logger.warn("Failed to retransform {}", className);
        }
    }
    
    private static String getDllPath() {
        java.net.URL dllUrl = getDllResource();
        if (dllUrl == null) {
            return Constants.DEFAULT_DLL_NAME;
        }
        
        return extractDllToTempFile();
    }
    
    private static java.net.URL getDllResource() {
        return AgentMain.class.getClassLoader().getResource(Constants.DEFAULT_DLL_NAME);
    }
    
    private static String extractDllToTempFile() {
        try (java.io.InputStream inputStream = getDllInputStream()) {
            if (inputStream == null) {
                return Constants.DEFAULT_DLL_NAME;
            }
            
            return writeDllToTempFile(inputStream);
        } catch (Exception e) {
            logger.error("Error extracting DLL: {}", e.getMessage());
            return Constants.DEFAULT_DLL_NAME;
        }
    }
    
    private static java.io.InputStream getDllInputStream() {
        return AgentMain.class.getClassLoader().getResourceAsStream(Constants.DEFAULT_DLL_NAME);
    }
    
    private static String writeDllToTempFile(java.io.InputStream inputStream) throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("TzdAgent", ".dll");
        tempFile.deleteOnExit();
        
        copyStreamToFile(inputStream, tempFile);
        return tempFile.getAbsolutePath();
    }
    
    private static void copyStreamToFile(java.io.InputStream inputStream, java.io.File file) throws Exception {
        try (java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file)) {
            byte[] buffer = new byte[Constants.BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
    
    private static final class Constants {
        private static final String DEFAULT_DLL_NAME = "TzdAgent.dll";
        private static final int BUFFER_SIZE = 1024;
        
        private Constants() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }
}