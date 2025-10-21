package com.zerwhit.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Hooks {
    private static ResourceLocation vapelogoTexture;
    private static ResourceLocation v4Texture;
    private static boolean texturesLoaded = false;

    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }

    private static void loadTextures() {
        try {
            vapelogoTexture = new ResourceLocation("zerwhit", "vapelogo.png");
            v4Texture = new ResourceLocation("zerwhit", "v4.png");
            texturesLoaded = true;
            System.out.println("Textures loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load textures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void onGameLoop() {
        Control.checkRShiftKey();
        Minecraft mc = Minecraft.getMinecraft();
        if (!texturesLoaded) {
            loadTextures();
        }
        
        int screenWidth = mc.displayWidth;
        int screenHeight = mc.displayHeight;

        try {
        drawFullScreenRect(screenWidth, screenHeight, 0.3F, 0.0F, 0.5F, 0.3F);
            System.out.println("Try to render logo.");
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (mc.getTextureManager().getTexture(vapelogoTexture) != null) {
                mc.getTextureManager().bindTexture(vapelogoTexture);
                drawTexturedRect(10, 10, 64, 64);
            }
            if (mc.getTextureManager().getTexture(v4Texture) != null) {
                mc.getTextureManager().bindTexture(v4Texture);
                drawTexturedRect(screenWidth - 74, 10, 64, 64);
            }
            GlStateManager.disableBlend();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void onPreTick() {
    }

    public static void onPostTick() {
    }

    public static void onPlayerPreUpdate() {
    }

    public static void onPlayerPostUpdate() {
    }
    
    private static void drawTexturedRect(int x, int y, int width, int height) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        double x1 = x;
        double y1 = y;
        double x2 = x + width;
        double y2 = y + height;
        
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x1, y2, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(x2, y2, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(x2, y1, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(x1, y1, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
    }
private static void drawFullScreenRect(int screenWidth, int screenHeight, float r, float g, float b, float alpha) throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.color(r, g, b, alpha);
    
    Tessellator tessellator = Tessellator.getInstance();
    WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
    worldrenderer.begin(7, DefaultVertexFormats.POSITION);
    worldrenderer.pos(0, screenHeight, 0.0D).endVertex();
    worldrenderer.pos(screenWidth, screenHeight, 0.0D).endVertex();
    worldrenderer.pos(screenWidth, 0, 0.0D).endVertex();
    worldrenderer.pos(0, 0, 0.0D).endVertex();
    tessellator.draw();
    
    GlStateManager.disableBlend();
    GlStateManager.enableTexture2D();
    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
}
}