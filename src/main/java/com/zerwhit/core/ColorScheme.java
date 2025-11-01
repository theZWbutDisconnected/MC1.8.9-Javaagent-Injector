package com.zerwhit.core;

public class ColorScheme {
    public int primary = 0xDD2980C8;
    public int secondary = 0xDD3498DB;
    public int accent = 0xDD2ECC71;
    public int background = 0xBB1A1A23;
    public int text = 0xFFFFFFFF;
    public int textDisabled = 0x64ECF0F1;
    public int moduleBackground = 0xBB2D2D37;
    public int moduleHover = 0xBB373741;
    public int arrayListBackground = 0xDD1A1A23;
    public int arrayListHeader = 0xDD2980C8;
    public int arrayListText = 0xFFFFFFFF;
    public int combatColor = 0xFFFF6B6B;
    public int movementColor = 0xFF4ECDC4;
    public int renderColor = 0xFF45B7D1;
    public int otherColor = 0xFFFFFFFF;

    public int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    public int mulAlpha(int rgba, float factor) {
        int originalAlpha = (rgba >>> 24) & 0xFF;
        int newAlpha = Math.min(255, Math.max(0, (int)(originalAlpha * factor)));
        return (newAlpha << 24) | (rgba & 0x00FFFFFF);
    }

    public int mulAlpha(int rgba, int factor) {
        int originalAlpha = (rgba >>> 24) & 0xFF;
        int newAlpha = (originalAlpha * factor) / 255;
        newAlpha = Math.min(255, Math.max(0, newAlpha));
        return (newAlpha << 24) | (rgba & 0x00FFFFFF);
    }
}
