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

    public int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
