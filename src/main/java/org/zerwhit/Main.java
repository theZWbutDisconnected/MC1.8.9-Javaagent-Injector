package org.zerwhit;

import org.tzd.agent.nativeapi.AgentNative;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("Starting agent injection process...");
        try {
            long pid = findMinecraftProcess();
            if (pid == -1) {
                logger.error("Could not find Minecraft process!");
                return;
            }
            logger.info("Found Minecraft process with PID: {}", pid);
            String jarPath = getCurrentJarPath();
            logger.info("Agent JAR path: {}", jarPath);
            String dllPath = getDllPath();
            System.load(dllPath);
            logger.info("Loaded TzdAgent.dll from: {}", dllPath);
            String result = AgentNative.agent_init(pid, jarPath, "com.zerwhit.AgentMain", args);
            if (result == null || result.isEmpty()) {
                logger.info("Agent injection successful!");
            } else {
                logger.error("Agent injection failed: {}", result);
            }
        } catch (Exception e) {
            logger.error("Error during injection: {}", e.getMessage());
            logger.error("Injection error details:", e);
        }
    }
    
    private static long findMinecraftProcess() {
        long pid = findPidByWindowTitle("1.8.9");
        if (pid != -1) {
            return pid;
        }
        pid = findPidByWindowTitle("Minecraft");
        if (pid != -1) {
            return pid;
        }
        pid = findPidByWindowTitle("Lunar");
        if (pid != -1) {
            return pid;
        }
        pid = findPidByWindowTitle("Badlion");
        if (pid != -1) {
            return pid;
        }
        
        return -1;
    }
    
    private static long findPidByWindowTitle(String windowTitle) {
        try {
            WinDef.HWND hwnd = findHWNDByWindowName(windowTitle);
            if (hwnd != null) {
                IntByReference pidRef = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidRef);
                return pidRef.getValue();
            }
        } catch (Exception e) {
            logger.warn("Error finding process by window title '{}': {}", windowTitle, e.getMessage());
        }
        return -1;
    }
    
    private static WinDef.HWND findHWNDByWindowName(String windowName) {
        final WinDef.HWND[] foundHwnd = {null};
        
        User32.INSTANCE.EnumWindows(new User32.WNDENUMPROC() {
            @Override
            public boolean callback(WinDef.HWND hwnd, com.sun.jna.Pointer data) {
                char[] windowText = new char[512];
                User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
                String title = new String(windowText).trim();
                
                if (title.contains(windowName) && User32.INSTANCE.IsWindowVisible(hwnd)) {
                    foundHwnd[0] = hwnd;
                    return false;
                }
                return true;
            }
        }, null);
        
        return foundHwnd[0];
    }
    
    private static String getCurrentJarPath() {
        try {
            String jarPath = new File(Main.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getAbsolutePath();
            if (jarPath.endsWith(".jar")) {
                return jarPath;
            } else {
                File tempJar = File.createTempFile("agent", ".jar");
                tempJar.deleteOnExit();
                return tempJar.getAbsolutePath();
            }
        } catch (Exception e) {
            logger.error("Error getting JAR path: {}", e.getMessage());
            return "agent.jar";
        }
    }
    
    private static String getDllPath() {
        java.net.URL dllUrl = Main.class.getClassLoader().getResource("TzdAgent.dll");
        if (dllUrl != null) {
            try {
                java.io.InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("TzdAgent.dll");
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