package com.zerwhit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

public class OldMain {
    private static final Logger logger = LogManager.getLogger(OldMain.class);
    
    public static WinDef.HWND hwnd;
    public static void main(String[] args)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {

        List<String> pids = findPidsByWindowName("1.8.9");
        List<WinDef.HWND> hwnds = findHWNDByWindowName("1.8.9");
        logger.info("Found {} Minecraft processes:", pids.size());
        for (String pid : pids) {
            logger.info("PID: {}", pid);
        }

        if (pids.isEmpty()) {
            logger.error("No Minecraft processes found!");
            return;
        }

        String pid = pids.get(0);
        hwnd = hwnds.get(0);
        logger.info("Attaching to PID: {}", pid);

        String path = getCurrentJarPath();
        logger.info("Using agent path: {}", path);

        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            logger.info("Loading agent...");
            vm.loadAgent(path, "useSystemClassLoader=true");
        } finally {
            vm.detach();
            logger.info("Detached from target VM");
        }
    }

    private static String getCurrentJarPath() {
        try {
            CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                File jarFile = new File(codeSource.getLocation().toURI());

                if (jarFile.isDirectory()) {
                    File projectDir = findProjectRoot(jarFile);
                    if (projectDir != null) {
                        File buildLibsDir = new File(projectDir, "build/libs");
                        if (buildLibsDir.exists() && buildLibsDir.isDirectory()) {
                            File[] jarFiles = buildLibsDir.listFiles((dir, name) ->
                                    name.endsWith(".jar") && !name.endsWith("-sources.jar") && !name.endsWith("-javadoc.jar")
                            );

                            if (jarFiles != null && jarFiles.length > 0) {
                                return jarFiles[jarFiles.length - 1].getAbsolutePath();
                            }
                        }
                    }
                    throw new RuntimeException("Cannot find JAR file in build/libs directory. Please build the project first.");
                } else {
                    return jarFile.getAbsolutePath();
                }
            }
        } catch (URISyntaxException e) {
            logger.error("Error getting current JAR path", e);
        }

        File currentDir = new File("");
        File jarFile = new File(currentDir, "build/libs/BadlionMain-1.0-SNAPSHOT.jar");
        if (jarFile.exists()) {
            return jarFile.getAbsolutePath();
        }

        throw new RuntimeException("Cannot determine agent JAR path");
    }

    private static File findProjectRoot(File classDir) {
        File current = classDir;
        while (current != null) {
            File buildDir = new File(current, "build");
            if (buildDir.exists() && buildDir.isDirectory()) {
                return current;
            }
            current = current.getParentFile();
        }
        return null;
    }

    private static List<String> findPidsByWindowName(String partialWindowName) {
        List<String> pids = new ArrayList<>();

        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            char[] windowText = new char[1024];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 1024);
            String title = Native.toString(windowText);

            if (!title.isEmpty() && title.toLowerCase().contains(partialWindowName.toLowerCase())) {
                IntByReference pidRef = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hWnd, pidRef);
                int pid = pidRef.getValue();
                WinDef.HMODULE kernel32 = Kernel32.INSTANCE.GetModuleHandle("kernel32");
                logger.debug("Found window: '{}' with PID: {}", title, pid);
                pids.add(String.valueOf(pid));
            }
            return true;
        }, null);

        return pids;
    }

    private static List<WinDef.HWND> findHWNDByWindowName(String partialWindowName) {
        List<WinDef.HWND> pids = new ArrayList<>();

        User32.INSTANCE.EnumWindows((hWnd, arg) -> {
            char[] windowText = new char[1024];
            User32.INSTANCE.GetWindowText(hWnd, windowText, 1024);
            String title = Native.toString(windowText);

            if (!title.isEmpty() && title.toLowerCase().contains(partialWindowName.toLowerCase())) {
                pids.add(hWnd);
            }
            return true;
        }, null);

        return pids;
    }
}