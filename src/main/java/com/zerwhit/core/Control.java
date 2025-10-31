package com.zerwhit.core;

import com.zerwhit.core.module.ClickGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Control {
    private static boolean wasRShiftPressed = false;
    private static boolean wasMousePressed = false;

    public static void checkRShiftKey() {
        boolean isRShiftPressed = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean isMousePressed = Mouse.isButtonDown(0);

        if (isRShiftPressed && !wasRShiftPressed) {
            onRShiftPressed();
        } else if (!isRShiftPressed && wasRShiftPressed) {
            onRShiftReleased();
        }

        if (isMousePressed && !wasMousePressed && Meta.clickGUIOpened) {
            int mouseX = getMouseX();
            int mouseY = getMouseY();
            ClickGUI.handleMouseClick(mouseX, mouseY);
        }

        wasRShiftPressed = isRShiftPressed;
        wasMousePressed = isMousePressed;
    }

    private static void onRShiftPressed() {
        Meta.clickGUIOpened = !Meta.clickGUIOpened;
        if (Meta.clickGUIOpened) {
            ClickGUI.onOpen();
        }
    }

    private static void onRShiftReleased() {
    }

    public static int getMouseX() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        return Mouse.getX() / scaledResolution.getScaleFactor();
    }

    public static int getMouseY() {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        return scaledResolution.getScaledHeight() - Mouse.getY() / scaledResolution.getScaleFactor();
    }
}