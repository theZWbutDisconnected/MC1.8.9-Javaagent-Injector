package com.zerwhit.core;

import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.manager.ModuleManager;
import com.zerwhit.core.resource.TextureResource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Hooks {
    private static boolean texturesInitialized = false;
    private static boolean modulesInitialized = false;
    private static final int MARGIN = 10;
    private static final ModuleManager moduleManager = ModuleManager.getInstance();

    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!modulesInitialized) {
            moduleManager.initialize();
            modulesInitialized = true;
        }
        moduleManager.invokeCategory(ModuleManager.ModuleCategory.RENDER, ModuleManager.ModuleHookType.TICK);
        
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
    }
    public static void onPlayerPostUpdate() {
        moduleManager.invokeCategory(ModuleManager.ModuleCategory.MOVEMENT, ModuleManager.ModuleHookType.TICK);
        moduleManager.invokeCategory(ModuleManager.ModuleCategory.COMBAT, ModuleManager.ModuleHookType.TICK);
        moduleManager.invokeCategory(ModuleManager.ModuleCategory.VISUAL, ModuleManager.ModuleHookType.TICK);
    }

    public static void onPlayerHurt() {
        moduleManager.invokeModule(ModuleManager.ModuleHookType.EVENT, "playerHurt");
    }

    private static void initializeTextures() {
        TextureRegistry.initialize();
        TextureLoader.loadAllTextureResources();

        texturesInitialized = true;
        System.out.println("Texture system initialized");
    }

    private static void render(int screenWidth, int screenHeight) {
        drawVapeIcons(screenWidth);
        int scaledWidth = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
        int scaledHeight = new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();
        moduleManager.invokeCategory(ModuleManager.ModuleCategory.RENDER, ModuleManager.ModuleHookType.RENDER, scaledWidth, scaledHeight);
    }

    private static void drawVapeIcons(int screenWidth) {
        TextureResource vapelogoResource = TextureRegistry.getTextureResource(TextureRegistry.CLIENTLOGO);
        TextureResource v4logoResource = TextureRegistry.getTextureResource(TextureRegistry.V4LOGO);
        int x = screenWidth - v4logoResource.getWidth() - MARGIN;
        Renderer.drawTexture(
                TextureRegistry.V4LOGO,
                x,
                MARGIN
        );
        Renderer.drawTexture(
                TextureRegistry.CLIENTLOGO,
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
        moduleManager.cleanup();
        texturesInitialized = false;
        modulesInitialized = false;
        System.out.println("Hooks system cleaned up");
    }

    /**
     * Hook method for intercepting ItemRenderer's renderItemInFirstPerson method
     * This method is called at the beginning of the original renderItemInFirstPerson method
     * Args: partialTickTime - the partial tick time for interpolation
     */
    public static void renderItemInFirstPersonHook(float partialTicks) {
        moduleManager.invokeCategory(ModuleManager.ModuleCategory.VISUAL, ModuleManager.ModuleHookType.VISUAL, partialTicks);
    }

    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }
}