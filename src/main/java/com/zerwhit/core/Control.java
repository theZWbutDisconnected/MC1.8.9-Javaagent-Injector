package com.zerwhit.core;

import org.lwjgl.input.Keyboard;

public class Control {
    private static boolean wasRShiftPressed = false;

    public static void checkRShiftKey() {
        boolean isRShiftPressed = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

        if (isRShiftPressed && !wasRShiftPressed) {
            onRShiftPressed();
        } else if (!isRShiftPressed && wasRShiftPressed) {
            onRShiftReleased();
        }

        wasRShiftPressed = isRShiftPressed;
    }

    private static void onRShiftPressed() {
        Meta.clickGUIOpened = !Meta.clickGUIOpened;
    }

    private static void onRShiftReleased() {
    }
}
