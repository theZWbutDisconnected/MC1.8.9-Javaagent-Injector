package com.zerwhit.core;

import com.zerwhit.core.screen.ClickGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Control {
    private static boolean wasRShiftPressed = false;
    private static boolean wasLeftMousePressed = false;
    private static boolean wasRightMousePressed = false;

    public static void checkRShiftKey() {
        boolean isRShiftPressed = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean isLeftMousePressed = Mouse.isButtonDown(0);
        boolean isRightMousePressed = Mouse.isButtonDown(1);

        if (isRShiftPressed && !wasRShiftPressed) {
            onRShiftPressed();
        } else if (!isRShiftPressed && wasRShiftPressed) {
            onRShiftReleased();
        }

        if (Meta.clickGUIOpened) {
            if (isLeftMousePressed && !wasLeftMousePressed) {
                int mouseX = getMouseX();
                int mouseY = getMouseY();
                ClickGUI.INSTANCE.handleMouseClick(mouseX, mouseY, 0);
            }
            
            if (isRightMousePressed && !wasRightMousePressed) {
                int mouseX = getMouseX();
                int mouseY = getMouseY();
                ClickGUI.INSTANCE.handleMouseClick(mouseX, mouseY, 1);
            }
        }

        wasRShiftPressed = isRShiftPressed;
        wasLeftMousePressed = isLeftMousePressed;
        wasRightMousePressed = isRightMousePressed;
    }

    private static void onRShiftPressed() {
        Meta.clickGUIOpened = !Meta.clickGUIOpened;
        Minecraft mc = Minecraft.getMinecraft();
        if (Meta.clickGUIOpened) {
            mc.mouseHelper.ungrabMouseCursor();
            mc.displayGuiScreen(ClickGUI.INSTANCE);
            ((GuiScreen)ClickGUI.INSTANCE).initGui();
            ClickGUI.INSTANCE.playOpenAnimation();
        } else {
            ((GuiScreen)ClickGUI.INSTANCE).onGuiClosed();
            mc.mouseHelper.grabMouseCursor();
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