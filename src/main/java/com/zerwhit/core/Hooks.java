package com.zerwhit.core;

import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.manager.ModuleManager;
import com.zerwhit.core.manager.ScreenEffects;
import com.zerwhit.core.manager.RotationManager;
import com.zerwhit.core.resource.TextureResource;
import com.zerwhit.core.util.ObfuscationReflectionHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Timer;

import java.io.IOException;

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
        moduleManager.invokeHook(ModuleManager.ModuleHookType.TICK);
        
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
        // 更新RotationManager
        RotationManager.getInstance().updateRotation();
        
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
        try {
            ScreenEffects.INSTANCE.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        texturesInitialized = true;
        System.out.println("Texture system and ScreenEffects initialized");
    }

    private static void render(int screenWidth, int screenHeight) {
        drawVapeIcons(screenWidth);
        int scaledWidth = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
        int scaledHeight = new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();
        Timer timer = (Timer) ObfuscationReflectionHelper.getObfuscatedFieldValue(Minecraft.class, new String[]{"timer", "field_71428_T"}, Minecraft.getMinecraft());
        float partialTicks = 0;
        if (timer != null) {
            partialTicks = timer.renderPartialTicks;
        }
        moduleManager.invokeHook(ModuleManager.ModuleHookType.RENDER, partialTicks, scaledWidth, scaledHeight);
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