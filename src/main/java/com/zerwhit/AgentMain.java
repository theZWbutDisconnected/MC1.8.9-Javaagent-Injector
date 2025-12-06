package com.zerwhit;

import org.tzd.agent.nativeapi.AgentNative;
import com.zerwhit.core.ClassTransformer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class AgentMain {
    public static void tzdAgentMain(AgentNative.AgentHandle handle, String agentArgs) throws Exception {
        System.out.println("[AgentMain] tzdAgentMain called with handle: " + handle);
        initAgent(handle, agentArgs);
    }
    
    private static void initAgent(AgentNative.AgentHandle handle, String agentArgs) {
        System.out.println("[AgentMain] Initializing agent with handle: " + handle);
        
        try {
            String dllPath = getDllPath();
            System.load(dllPath);
            System.out.println("[AgentMain] Loaded TzdAgent.dll from: " + dllPath);
            
            if (handle != null) {
                AgentNative.addTransformer(handle, new AgentNative.ClassFileTransformerEncapsulation(
                    new ClassTransformer(), true
                ));
                
                System.out.println("[AgentMain] Transformer added successfully");
                
                try {
                    Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                    System.out.println("[AgentMain] Retransformed Minecraft class");
                } catch (ClassNotFoundException e) {
                    System.out.println("[AgentMain] Minecraft class not found, skipping retransform");
                }
            } else {
                System.out.println("[AgentMain] Using traditional instrumentation");
            }
            
            System.out.println("[AgentMain] Agent initialization completed successfully");
            
        } catch (Exception e) {
            System.err.println("[AgentMain] Error during agent initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void retransformLoadedClasses(AgentNative.AgentHandle handle) {
        for (Class<?> clazz : handle.getAllLoadedClasses()) {
            String className = clazz.getName().replace('.', '/');
            if (ClassTransformer.isMCClass(className) && handle.isModifiableClass(clazz)) {
                try {
                    AgentNative.retransformClasses(handle, clazz);
                } catch (Exception e) {
                    System.err.println("Failed to retransform " + className);
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
                System.err.println("[AgentMain] Error extracting DLL: " + e.getMessage());
            }
        }

        return "TzdAgent.dll";
    }
}