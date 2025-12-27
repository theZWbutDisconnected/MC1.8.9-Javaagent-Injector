package org.zerwhit.core.module.visual;

import javafx.scene.input.KeyCode;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.IVisualModule;
import org.zerwhit.core.module.ModuleBase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import org.lwjgl.input.Mouse;

import static org.zerwhit.core.util.ObfuscationReflectionHelper.*;

public class ModuleLegacyAnim extends ModuleBase implements IVisualModule, ITickableModule {
    public ModuleLegacyAnim() {
        super("LegacyAnim", true, "Visual");
        addConfig("HeldItemSize", 1.0);
        addConfig("XOffset", 0.0);
        addConfig("YOffset", 0.0);
        addConfig("ZOffset", 0.0);
    }
    
    public void onHook(String funcName, float partialTicks) {
        if (!funcName.equals("renderItemInFirstPerson")) return;

        double itemSize = (Double) getConfig("HeldItemSize");

        ItemRenderer renderer = Minecraft.getMinecraft().getItemRenderer();
        Object prevEquippedProgressObj = getObfuscatedFieldValue(ItemRenderer.class, new String[]{"prevEquippedProgress", "field_78451_d"}, renderer);
        Object equippedProgressObj = getObfuscatedFieldValue(ItemRenderer.class, new String[]{"equippedProgress", "field_78454_c"}, renderer);
        float prevEquippedProgress = prevEquippedProgressObj != null ? (float)prevEquippedProgressObj : 0.0F;
        float equippedProgress = equippedProgressObj != null ? (float)equippedProgressObj : 0.0F;
        
        float f = 0.0F;//1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
        AbstractClientPlayer abstractclientplayer = Minecraft.getMinecraft().thePlayer;
        float f1 = abstractclientplayer.getSwingProgress(partialTicks);
        float f2 = rotMng.rendererViewEntity.rotationPitch;
        float f3 = rotMng.rendererViewEntity.rotationYaw;
        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"rotateArroundXAndY", "func_178101_a"}, renderer, f2, f3);
        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"setLightMapFromPlayer", "func_178109_a"}, renderer, abstractclientplayer);
        invokeObfuscatedMethod(ItemRenderer.class, new String[]{"rotateWithPlayerRotations", "func_178110_a"}, renderer, (EntityPlayerSP)abstractclientplayer, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.translate((Double) getConfig("XOffset"), (Double) getConfig("YOffset"), (Double) getConfig("ZOffset"));
        ItemStack itemToRenderObj = (ItemStack) getObfuscatedFieldValue(ItemRenderer.class, new String[]{"itemToRender", "field_78453_b"}, renderer);
        if (itemToRenderObj != null)
        {
            Item itemObj = itemToRenderObj.getItem();
            boolean shouldBlock = (Mouse.isButtonDown(1) && itemObj instanceof ItemSword) || Meta.blockRenderEnabled;
            if (itemObj == Items.filled_map)
            {
                invokeObfuscatedMethod(ItemRenderer.class, new String[]{"renderItemMap", "func_178097_a"}, renderer, abstractclientplayer, f2, f, f1);
            }
            else if (shouldBlock || abstractclientplayer.getItemInUseCount() > 0)
            {
                EnumAction enumaction = ((ItemStack)itemToRenderObj).getItemUseAction();
                if (shouldBlock) enumaction = EnumAction.BLOCK;
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
                        GlStateManager.translate(0.0f, 0.15f, 0.0f);
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

            GlStateManager.scale(itemSize, itemSize, itemSize);
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

    @Override
    public void onModuleTick() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Meta.legacyAnimEnabled = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Meta.legacyAnimEnabled = false;
    }

    @Override
    public double getMaxValueForConfig(String key) {
        if (key == "HeldItemSize") return 2.0;
        if (key == "XOffset") return 2.0;
        if (key == "YOffset") return 2.0;
        if (key == "ZOffset") return 2.0;
        return super.getMaxValueForConfig(key);
    }

    @Override
    public double getMinValueForConfig(String key) {
        if (key == "XOffset") return -2.0;
        if (key == "YOffset") return -2.0;
        if (key == "ZOffset") return -2.0;
        return super.getMinValueForConfig(key);
    }
}
