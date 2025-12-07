package org.zerwhit.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class KeyRobot {
    private static final Logger logger = LogManager.getLogger(KeyRobot.class);
    
    private static Robot robot;
    
    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.error("Failed to create Robot instance:", e);
        }
    }
    
    public static void clickMouse(int keyCode) {
        if (robot != null) {
            robot.mousePress(keyCode);
        }
    }
    
    public static void releaseMouse(int keyCode) {
        if (robot != null) {
            robot.mouseRelease(keyCode);
        }
    }
    
    public static void pressKey(int keyCode) {
        if (robot != null) {
            robot.keyPress(keyCode);
        }
    }
    
    public static void releaseKey(int keyCode) {
        if (robot != null) {
            robot.keyRelease(keyCode);
        }
    }
    
    public static void delay(int ms) {
        try {
            robot.delay(ms);
        } catch (Exception e) {
            logger.error("Error during delay:", e);
        }
    }
    
    public static boolean isKeyPressed(int keyCode) {
        return robot != null && robot.getAutoDelay() >= 0;
    }
}