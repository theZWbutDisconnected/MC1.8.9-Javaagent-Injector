package com.zerwhit.core;

import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import com.zerwhit.core.module.combat.ModuleAutoBlock;
import com.zerwhit.core.module.IRenderModule;
import com.zerwhit.core.module.IVisualModule;
import com.zerwhit.core.resource.TextureResource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.List;
import java.util.Objects;

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
    }
    public static void onPlayerPostUpdate() {
        triggerModuleTick("Movement");
        triggerModuleTick("Combat");
    }

    public static void onPlayerHurt() {
        triggerAutoBlock();
    }

    private static void triggerAutoBlock() {
        try {
            List<ModuleBase> combatModules = ModuleBase.categories.get("Combat");
            for (ModuleBase module : combatModules) {
                if (module instanceof ModuleAutoBlock) {
                    ((ModuleAutoBlock) module).onPlayerHurt();
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error in AutoBlock: " + e.getMessage());
        }
    }

    private static void initializeTextures() {
        TextureRegistry.initialize();
        TextureLoader.loadAllTextureResources();

        texturesInitialized = true;
        System.out.println("Texture system initialized");
    }

    private static void render(int screenWidth, int screenHeight) {
        drawVapeIcons(screenWidth);
        for (ModuleBase module : ModuleBase.categories.get("Render")) {
            if (module instanceof IRenderModule && module.enabled) {
                ((IRenderModule) module).onRender(new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth(), new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight());
                break;
            }
        }
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
        texturesInitialized = false;
        System.out.println("Hooks texture system cleaned up");
    }

    /**
     * Hook method for intercepting ItemRenderer's renderItemInFirstPerson method
     * This method is called at the beginning of the original renderItemInFirstPerson method
     * Args: partialTickTime - the partial tick time for interpolation
     */
    public static void renderItemInFirstPersonHook(float partialTicks) {
        triggerVisualModule("LegacyAnim", partialTicks);
    }

    private static void triggerModuleTick() {
        for (String key : ModuleBase.categories.keySet()) {
            triggerModuleTick(key);
        }
    }

    private static void triggerModuleTick(String categorie) {
        List<ModuleBase> modules = ModuleBase.categories.get(categorie);
        for (ModuleBase module : modules) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(module instanceof ITickableModule) || !module.enabled || mc.theWorld == null || mc.thePlayer == null) return;
            ((ITickableModule)module).onModuleTick();
        }
    }

    private static void triggerVisualModule(String name, float partialTicks) {
        List<ModuleBase> modules = ModuleBase.categories.get("Visual");
        for (ModuleBase module : modules) {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(module instanceof IVisualModule) || !Objects.equals(module.name, name) || !module.enabled || mc.theWorld == null || mc.thePlayer == null) return;
            ((IVisualModule)module).onHook(partialTicks);
        }
    }

    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }
}