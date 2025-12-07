package org.zerwhit.core.module.visual;

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
    }
    
    public void onHook(String funcName, float partialTicks) {
        if (!funcName.equals("renderItemInFirstPerson")) return;
        ItemRenderer renderer = Minecraft.getMinecraft().getItemRenderer();
        Object prevEquippedProgressObj = getObfuscatedFieldValue(ItemRenderer.class, new String[]{"prevEquippedProgress", "field_78451_d"}, renderer);
        Object equippedProgressObj = getObfuscatedFieldValue(ItemRenderer.class, new String[]{"equippedProgress", "field_78454_c"}, renderer);
        float prevEquippedProgress = prevEquippedProgressObj != null ? (float)prevEquippedProgressObj : 0.0F;
        float equippedProgress = equippedProgressObj != null ? (float)equippedProgressObj : 0.0F;
        
        float f = 1.0F - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
        AbstractClientPlayer abstractclientplayer = Minecraft.getMinecraft().thePlayer;
        float f1 = abstractclientplayer.getSwingProgress(partialTicks);
        float f2 = rotMng.rendererViewEntity.prevRotationPitch + (rotMng.rendererViewEntity.rotationPitch - rotMng.rendererViewEntity.prevRotationPitch) * partialTicks;
        float f3 = rotMng.rendererViewEntity.prevRotationYaw + (rotMng.rendererViewEntity.rotationYaw - rotMng.rendererViewEntity.prevRotationYaw) * partialTicks;
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
                if (itemObj instanceof ItemSword && Mouse.isButtonDown(1)) enumaction = EnumAction.BLOCK;
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

    @Override
    public void onModuleTick() {
    }

    @Override
    public void onEnable() {
        Meta.legacyAnimEnabled = true;
    }

    @Override
    public void onDisable() {
        Meta.legacyAnimEnabled = false;
    }
}
