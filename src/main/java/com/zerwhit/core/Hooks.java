package com.zerwhit.core;

import com.zerwhit.AgentMain;
import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRenderer;
import net.minecraft.client.Minecraft;

public class Hooks {
    public static int VAPELOGO_TEXTURE_ID = -1;
    public static int V4_TEXTURE_ID = -1;

    private static final String VAPELOGO_PATH = "assets/zerwhit/textures/vapelogo.png";
    private static final String V4LOGO_PATH = "assets/zerwhit/textures/v4.png";
    private static final int VAPELOGO_WIDTH = 178;
    private static final int VAPELOGO_HEIGHT = 53;
    private static final int V4LOGO_WIDTH = 76;
    private static final int V4LOGO_HEIGHT = 53;
    private static final int MARGIN = 10;

    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();

        try {
            if (VAPELOGO_TEXTURE_ID == -1 || V4_TEXTURE_ID == -1) {
                loadTextures();
            }

            renderTextures(mc.displayWidth, mc.displayHeight);
        } catch (Exception e) {
            System.err.println("Failed to render display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void onGameLoop() {
        Control.checkRShiftKey();
    }

    public static void onPreTick() {}
    public static void onPostTick() {}
    public static void onPlayerPreUpdate() {}
    public static void onPlayerPostUpdate() {}

    private static void loadTextures() {
        if (VAPELOGO_TEXTURE_ID == -1) {
            VAPELOGO_TEXTURE_ID = TextureLoader.loadTextureFromResource(VAPELOGO_PATH, "vapelogo");
        }
        if (V4_TEXTURE_ID == -1) {
            V4_TEXTURE_ID = TextureLoader.loadTextureFromResource(V4LOGO_PATH, "v4logo");
        }
    }

    private static void renderTextures(int screenWidth, int screenHeight) {
        int vapelogoX = MARGIN;
        int vapelogoY = MARGIN;
        TextureRenderer.drawTexture(VAPELOGO_TEXTURE_ID, vapelogoX, vapelogoY, VAPELOGO_WIDTH, VAPELOGO_HEIGHT);

        int v4logoX = vapelogoX + VAPELOGO_WIDTH + MARGIN;
        int v4logoY = MARGIN;
        TextureRenderer.drawTexture(V4_TEXTURE_ID, v4logoX, v4logoY, V4LOGO_WIDTH, V4LOGO_HEIGHT);
    }

    public static void cleanup() {
        TextureLoader.releaseTexture(VAPELOGO_TEXTURE_ID);
        TextureLoader.releaseTexture(V4_TEXTURE_ID);
        VAPELOGO_TEXTURE_ID = -1;
        V4_TEXTURE_ID = -1;
    }

    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }
}