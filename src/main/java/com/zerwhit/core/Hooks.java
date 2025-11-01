package com.zerwhit.core;

import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.module.Module;
import com.zerwhit.core.module.combat.ModuleAutoBlock;
import com.zerwhit.core.resource.TextureResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import static com.zerwhit.core.screen.ClickGUI.colorScheme;

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
            List<Module> combatModules = Module.categories.get("Combat");
            for (Module module : combatModules) {
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
        
        // Call render method for Arraylist module if enabled
        for (Module module : Module.categories.get("Render")) {
            if (module instanceof com.zerwhit.core.module.render.ModuleArraylist && module.enabled) {
                module.render(new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth(), screenHeight);
                break;
            }
        }
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
        for (String key : Module.categories.keySet()) {
            triggerModuleTick(key);
        }
    }

    private static void triggerModuleTick(String categorie) {
        List<Module> modules = Module.categories.get(categorie);
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