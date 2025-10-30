package com.zerwhit.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class Hooks {
    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }
    
    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();

        try {
            drawFullScreenRect(mc.displayWidth, mc.displayHeight, 0.3F, 0.0F, 0.5F, 0.3F);
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
    public static void onPlayerPreUpdate() {}
    public static void onPlayerPostUpdate() {}

    private static void drawFullScreenRect(int width, int height, float r, float g, float b, float alpha) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(r, g, b, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(0, height, 0.0D).endVertex();
        worldrenderer.pos(width, height, 0.0D).endVertex();
        worldrenderer.pos(width, 0, 0.0D).endVertex();
        worldrenderer.pos(0, 0, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawVapelogo(int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getMinecraft();
        // mc.getTextureManager().bindTexture(VAPELOGO_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        int logoWidth = 178;
        int logoHeight = 53;
        int margin = 10;
        int x = screenWidth - logoWidth - margin;
        int y = margin;
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + logoHeight, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(x + logoWidth, y + logoHeight, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(x + logoWidth, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}