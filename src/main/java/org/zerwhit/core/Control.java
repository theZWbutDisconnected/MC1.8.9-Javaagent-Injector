package org.zerwhit.core;

import org.zerwhit.core.data.Meta;
import org.zerwhit.core.screen.ClickGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class Control {
    
    private static final class InputState {
        private boolean rShiftPressed = false;
        private boolean leftMousePressed = false;
        private boolean rightMousePressed = false;
        
        private InputState() {
            // Private constructor to prevent instantiation
        }
    }
    
    private static final InputState INPUT_STATE = new InputState();
    
    private Control() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    public static void checkRShiftKey() {
        boolean currentRShiftPressed = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean currentLeftMousePressed = Mouse.isButtonDown(0);
        boolean currentRightMousePressed = Mouse.isButtonDown(1);
        
        handleRShiftStateChange(currentRShiftPressed);
        handleMouseInput(currentLeftMousePressed, currentRightMousePressed);
        
        updateInputState(currentRShiftPressed, currentLeftMousePressed, currentRightMousePressed);
    }
    
    private static void handleRShiftStateChange(boolean currentRShiftPressed) {
        if (currentRShiftPressed && !INPUT_STATE.rShiftPressed) {
            onRShiftPressed();
        } else if (!currentRShiftPressed && INPUT_STATE.rShiftPressed) {
            onRShiftReleased();
        }
    }
    
    private static void handleMouseInput(boolean currentLeftMousePressed, boolean currentRightMousePressed) {
        if (!Meta.clickGUIOpened) {
            return;
        }
        
        handleLeftMouseClick(currentLeftMousePressed);
        handleRightMouseClick(currentRightMousePressed);
    }
    
    private static void handleLeftMouseClick(boolean currentLeftMousePressed) {
        if (currentLeftMousePressed && !INPUT_STATE.leftMousePressed) {
            int mouseX = getMouseX();
            int mouseY = getMouseY();
            ClickGUI.INSTANCE.handleMouseClick(mouseX, mouseY, 0);
        }
    }
    
    private static void handleRightMouseClick(boolean currentRightMousePressed) {
        if (currentRightMousePressed && !INPUT_STATE.rightMousePressed) {
            int mouseX = getMouseX();
            int mouseY = getMouseY();
            ClickGUI.INSTANCE.handleMouseClick(mouseX, mouseY, 1);
        }
    }
    
    private static void updateInputState(boolean rShiftPressed, boolean leftMousePressed, boolean rightMousePressed) {
        INPUT_STATE.rShiftPressed = rShiftPressed;
        INPUT_STATE.leftMousePressed = leftMousePressed;
        INPUT_STATE.rightMousePressed = rightMousePressed;
    }
    
    private static void onRShiftPressed() {
        Meta.clickGUIOpened = !Meta.clickGUIOpened;
        Minecraft minecraft = Minecraft.getMinecraft();
        
        if (Meta.clickGUIOpened) {
            openClickGUI(minecraft);
        } else {
            closeClickGUI(minecraft);
        }
    }
    
    private static void onRShiftReleased() {
    }
    
    private static void openClickGUI(Minecraft minecraft) {
        minecraft.mouseHelper.ungrabMouseCursor();
        minecraft.displayGuiScreen(ClickGUI.INSTANCE);
        ((GuiScreen)ClickGUI.INSTANCE).initGui();
        ClickGUI.INSTANCE.playOpenAnimation();
    }
    
    private static void closeClickGUI(Minecraft minecraft) {
        ((GuiScreen)ClickGUI.INSTANCE).onGuiClosed();
        minecraft.mouseHelper.grabMouseCursor();
    }
    
    public static int getMouseX() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        return Mouse.getX() / scaledResolution.getScaleFactor();
    }
    
    public static int getMouseY() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        return scaledResolution.getScaledHeight() - Mouse.getY() / scaledResolution.getScaleFactor();
    }
}