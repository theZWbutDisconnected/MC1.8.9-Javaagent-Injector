package com.zerwhit.core.jni;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

public interface GraphicLib extends StdCallLibrary {
    GraphicLib INSTANCE = null;

    void DrawPNG(HWND hwnd, WString filePath, int x, int y, int width, int height);
}