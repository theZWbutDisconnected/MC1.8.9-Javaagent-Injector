package com.zerwhit.core;

import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.manager.TextureRenderer;
import com.zerwhit.core.resource.TextureResource;
import net.minecraft.client.Minecraft;

public class Hooks {
    private static boolean texturesInitialized = false;

    private static final int MARGIN = 10;

    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();

        try {
            if (!texturesInitialized) {
                initializeTextures();
            }

            if (TextureRegistry.isTextureLoaded(TextureRegistry.VAPELOGO) &&
                    TextureRegistry.isTextureLoaded(TextureRegistry.V4LOGO)) {
                renderTextures(mc.displayWidth, mc.displayHeight);
            } else {
                TextureLoader.loadAllTextureResources();
            }
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

    private static void initializeTextures() {
        TextureRegistry.initialize();
        TextureLoader.loadAllTextureResources();

        texturesInitialized = true;
        System.out.println("Texture system initialized");
    }

    private static void renderTextures(int screenWidth, int screenHeight) {
        TextureResource vapelogoResource = TextureRegistry.getTextureResource(TextureRegistry.VAPELOGO);
        TextureResource v4logoResource = TextureRegistry.getTextureResource(TextureRegistry.V4LOGO);

        if (vapelogoResource != null && vapelogoResource.isLoaded()) {
            int vapelogoX = MARGIN;
            int vapelogoY = MARGIN;
            TextureRenderer.drawTexture(
                    vapelogoResource.getTextureId(),
                    vapelogoX, vapelogoY,
                    vapelogoResource.getWidth(),
                    vapelogoResource.getHeight()
            );
        }

        if (v4logoResource != null && v4logoResource.isLoaded()) {
            int v4logoX = MARGIN + (vapelogoResource != null ? vapelogoResource.getWidth() + MARGIN : MARGIN);
            int v4logoY = MARGIN;
            TextureRenderer.drawTexture(
                    v4logoResource.getTextureId(),
                    v4logoX, v4logoY,
                    v4logoResource.getWidth(),
                    v4logoResource.getHeight()
            );
        }
    }

    public static TextureResource getTextureResource(String key) {
        return TextureRegistry.getTextureResource(key);
    }

    public static boolean isTextureLoaded(String key) {
        return TextureRegistry.isTextureLoaded(key);
    }

    public static void cleanup() {
        TextureRegistry.cleanup();
        texturesInitialized = false;
        System.out.println("Hooks texture system cleaned up");
    }

    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }
}