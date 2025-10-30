package com.zerwhit.core.manager;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class TextureRenderer {
    public static void drawTexture(int textureId, int x, int y, int width, int height) {
        if (textureId == -1) {
            return;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawTexture(int textureId, int x, int y, int width, int height, float alpha) {
        if (textureId == -1) {
            return;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}