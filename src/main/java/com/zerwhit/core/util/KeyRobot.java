package com.zerwhit.core.util;

import java.awt.*;

public class KeyRobot {
    private static Robot robot;
    
    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
    
    public static boolean isKeyPressed(int keyCode) {
        return robot != null && robot.getAutoDelay() >= 0;
    }
}