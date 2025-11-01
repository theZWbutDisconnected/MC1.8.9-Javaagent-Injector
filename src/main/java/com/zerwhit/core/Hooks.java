package com.zerwhit.core;

import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.module.Module;
import com.zerwhit.core.resource.TextureResource;
import com.zerwhit.core.screen.ClickGUI;
import net.minecraft.client.Minecraft;

import java.util.List;

public class Hooks {
    private static boolean texturesInitialized = false;
    private static final int MARGIN = 10;

    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();

        triggerModuleTick("Render");
        try {
            if (!texturesInitialized) {
                initializeTextures();
            }

            if (TextureRegistry.isTextureLoaded(TextureRegistry.VAPELOGO) &&
                    TextureRegistry.isTextureLoaded(TextureRegistry.V4LOGO)) {
                render(mc.displayWidth, mc.displayHeight);
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
    public static void onPlayerPreUpdate() {
        triggerModuleTick("Movement");
    }
    public static void onPlayerPostUpdate() {
        triggerModuleTick("Combat");
    }

    private static void initializeTextures() {
        TextureRegistry.initialize();
        TextureLoader.loadAllTextureResources();

        texturesInitialized = true;
        System.out.println("Texture system initialized");
    }

    private static void render(int screenWidth, int screenHeight) {
        drawVapeIcons(screenWidth);
    }

    private static void drawVapeIcons(int screenWidth) {
        TextureResource vapelogoResource = TextureRegistry.getTextureResource(TextureRegistry.VAPELOGO);
        TextureResource v4logoResource = TextureRegistry.getTextureResource(TextureRegistry.V4LOGO);
        int x = screenWidth - v4logoResource.getWidth() - MARGIN;
        Renderer.drawTexture(
                TextureRegistry.V4LOGO,
                x,
                MARGIN
        );
        Renderer.drawTexture(
                TextureRegistry.VAPELOGO,
                x - vapelogoResource.getWidth(), MARGIN
        );
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

    private static void triggerModuleTick() {
        for (String key : ClickGUI.categories.keySet()) {
            triggerModuleTick(key);
        }
    }

    private static void triggerModuleTick(String categorie) {
        List<Module> modules = ClickGUI.categories.get(categorie);
        for (Module module : modules) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!module.enabled || mc.theWorld == null || mc.thePlayer == null) return;
            module.onModuleTick();
        }
    }

    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }
}