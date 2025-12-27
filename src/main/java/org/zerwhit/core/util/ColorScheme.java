package org.zerwhit.core.util;

public class ColorScheme {
    public int primary = 0xDD8A2BE2;
    public int secondary = 0xDD9B4DF3;
    public int accent = 0xDDBA55D3;
    public int background = 0xDD0F0F15;
    public int text = 0xFFFFFFFF;
    public int textDisabled = 0x88A0A0A0;
    public int moduleBackground = 0xDD1A1A23;
    public int moduleHover = 0xDD252535;
    public int arrayListBackground = 0xDD0F0F15;
    public int arrayListHeader = 0xDD8A2BE2;
    public int arrayListText = 0xFFFFFFFF;
    public int combatColor = 0xFFFF6B6B;
    public int movementColor = 0xFF4ECDC4;
    public int renderColor = 0xFF45B7D1;
    public int visualColor = 0xFFCD6DFF;
    public int otherColor = 0xFFFFFFFF;
    public int dynamicIslandBackground = 0x221E0A2B;
    public int dynamicIslandAccent = 0xFF8A2BE2;
    public int dynamicIslandText = 0xFFFFFFFF;
    public int dynamicIslandBorder = 0x886A1B9A;

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
