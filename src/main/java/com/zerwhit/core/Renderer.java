package com.zerwhit.core;

import com.zerwhit.core.manager.TextureRegistry;
import com.zerwhit.core.resource.TextureResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class Renderer {
    public static void drawTexture(String textureName, int x, int y) {
        drawTexture(textureName, x, y, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawTexture(String textureName, int x, int y, float r, float g, float b, float alpha) {
        TextureResource resource = TextureRegistry.getTextureResource(textureName);
        int width = resource.getWidth();
        int height = resource.getHeight();
        int textureId = resource.getTextureId();
        if (textureId == -1) {
            return;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(r, g, b, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution scale = new ScaledResolution(mc);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos((double) x / scale.getScaleFactor(), (double) (y + height) / scale.getScaleFactor(), 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos((double) (x + width) / scale.getScaleFactor(), (double) (y + height) / scale.getScaleFactor(), 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos((double) (x + width) / scale.getScaleFactor(), (double) y / scale.getScaleFactor(), 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos((double) x / scale.getScaleFactor(), (double) y / scale.getScaleFactor(), 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRect(int x, int y, int width, int height, int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        drawRect(x + radius, y, width - radius * 2, height, color);
        drawRect(x, y + radius, width, height - radius * 2, color);

        drawCircle(x + radius, y + radius, radius, color);
        drawCircle(x + width - radius, y + radius, radius, color);
        drawCircle(x + radius, y + height - radius, radius, color);
        drawCircle(x + width - radius, y + height - radius, radius, color);
    }

    public static void drawCircle(int x, int y, int radius, int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        worldrenderer.begin(6, DefaultVertexFormats.POSITION);
        worldrenderer.pos(x, y, 0.0D).endVertex();

        for (int i = 0; i <= 360; i++) {
            double angle = Math.PI * i / 180.0;
            worldrenderer.pos(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius, 0.0D).endVertex();
        }

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    // 绘制带阴影的文字
    public static void drawStringWithShadow(String text, int x, int y, int color) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        fontRenderer.drawStringWithShadow(text, x, y, color);
    }

    // 绘制渐变矩形
    public static void drawGradientRect(int x, int y, int width, int height, int startColor, int endColor) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >> 8 & 255) / 255.0F;
        float startBlue = (float)(startColor & 255) / 255.0F;
        float endAlpha = (float)(endColor >> 24 & 255) / 255.0F;
        float endRed = (float)(endColor >> 16 & 255) / 255.0F;
        float endGreen = (float)(endColor >> 8 & 255) / 255.0F;
        float endBlue = (float)(endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x + width, y, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(x, y, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldrenderer.pos(x, y + height, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}