package com.zerwhit.core;

import com.zerwhit.core.manager.TextureLoader;
import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.module.Module;
import com.zerwhit.core.module.combat.ModuleAutoBlock;
import com.zerwhit.core.resource.TextureResource;
import com.zerwhit.core.util.ObfuscationReflectionHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.zerwhit.core.screen.ClickGUI.colorScheme;
import static com.zerwhit.core.util.ObfuscationReflectionHelper.*;

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
        for (Module module : Module.categories.get("Render")) {
            if (module instanceof com.zerwhit.core.module.render.ModuleArraylist && module.enabled) {
                module.render(new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth(), screenHeight);
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
        ItemRenderer renderer = Minecraft.getMinecraft().getItemRenderer();
        Object prevEquippedProgressObj = getObfuscatedFieldValue(ItemRenderer.class, new String[]{"prevEquippedProgress", "field_78451_d"}, renderer);
        Object equippedProgressObj = getObfuscatedFieldValue(ItemRenderer.class, new String[]{"equippedProgress", "field_78454_c"}, renderer);
        float prevEquippedProgress = prevEquippedProgressObj != null ? (float)prevEquippedProgressObj : 0.0F;
        float equippedProgress = equippedProgressObj != null ? (float)equippedProgressObj : 0.0F;
        
        float f = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
        AbstractClientPlayer abstractclientplayer = Minecraft.getMinecraft().thePlayer;
        float f1 = abstractclientplayer.getSwingProgress(partialTicks);
        float f2 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
        float f3 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"rotateArroundXAndY", "func_178101_a"}, renderer, f2, f3);
        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"setLightMapFromPlayer", "func_178109_a"}, renderer, abstractclientplayer);
        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"rotateWithPlayerRotations", "func_178110_a"}, renderer, (EntityPlayerSP)abstractclientplayer, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        ItemStack itemToRenderObj = (ItemStack) getObfuscatedFieldValue(ItemRenderer.class, new String[]{"itemToRender", "field_78453_b"}, renderer);
        if (itemToRenderObj != null)
        {
            Item itemObj = itemToRenderObj.getItem();
            if (itemObj == Items.filled_map)
            {
                invokeObfuscatedMethod(ItemRenderer.class, new String[]{"renderItemMap", "func_178097_a"}, renderer, abstractclientplayer, f2, f, f1);
            }
            else if (abstractclientplayer.getItemInUseCount() > 0)
            {
                EnumAction enumaction = ((ItemStack)itemToRenderObj).getItemUseAction();

                switch (enumaction)
                {
                    case NONE:
                        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"transformFirstPersonItem", "func_178096_b"}, renderer, f, f1);
                        break;

                    case EAT:
                    case DRINK:
                        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"performDrinking", "func_178104_a"}, renderer, abstractclientplayer, partialTicks);
                        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"transformFirstPersonItem", "func_178096_b"}, renderer, f, f1);
                        break;

                    case BLOCK:
                        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"transformFirstPersonItem", "func_178096_b"}, renderer, f, f1);
                        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"doBlockTransformations", "func_178103_d"}, renderer);
                        break;

                    case BOW:
                        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"transformFirstPersonItem", "func_178096_b"}, renderer, f, f1);
                        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"doBowTransformations", "func_178098_a"}, renderer, partialTicks, abstractclientplayer);
                        break;
                }
            }
            else
            {
                invokeObfuscatedMethod(ItemRenderer.class, new String[]{"doItemUsedTransformations", "func_178105_d"}, renderer, f1);
                invokeObfuscatedMethod(ItemRenderer.class, new String[]{"transformFirstPersonItem", "func_178096_b"}, renderer, f, f1);
            }

            invokeObfuscatedMethod(ItemRenderer.class, new String[]{"renderItem", "func_178099_a"}, renderer, abstractclientplayer, itemToRenderObj, ItemCameraTransforms.TransformType.FIRST_PERSON);
        }
        else if (!abstractclientplayer.isInvisible())
        {
            invokeObfuscatedMethod(ItemRenderer.class, new String[]{"renderPlayerArm", "func_178095_a"}, renderer, abstractclientplayer, f, f1);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
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