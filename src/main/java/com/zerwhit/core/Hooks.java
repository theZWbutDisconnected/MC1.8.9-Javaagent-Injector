package com.zerwhit.core;

import com.zerwhit.AgentMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.File;

public class Hooks {
    private static ResourceLocation VAPELOGO_TEXTURE = new ResourceLocation("zerwhit", "textures/vapelogo.png");
    private static boolean resourcesInitialized = false;
    private static boolean textureLoaded = false;
    
    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }

    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();

        try {
            if (!resourcesInitialized) {
                addAgentResourcesToResourceManager();
                resourcesInitialized = true;
            }

            if (!textureLoaded) {
                try {
                    mc.getTextureManager().bindTexture(VAPELOGO_TEXTURE);
                    textureLoaded = true;
                    System.out.println("Successfully loaded agent texture: " + VAPELOGO_TEXTURE);
                } catch (Exception e) {
                    System.err.println("Failed to load agent texture: " + e.getMessage());
                    return;
                }
            }

            drawFullScreenRect(mc.displayWidth, mc.displayHeight, 0.3F, 0.0F, 0.5F, 0.3F);
            drawVapelogo(mc.displayWidth, mc.displayHeight);
        } catch (Exception e) {
            System.err.println("Failed to render display: " + e.getMessage());
        }
    }

    private static void addAgentResourcesToResourceManager() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getResourceManager() instanceof SimpleReloadableResourceManager) {
                SimpleReloadableResourceManager rsm = (SimpleReloadableResourceManager) mc.getResourceManager();
                File agentJarFile = new File(AgentMain.class.getProtectionDomain()
                        .getCodeSource().getLocation().getPath());
                AgentJarResourcePack agentPack = new AgentJarResourcePack(agentJarFile);

                rsm.reloadResourcePack(agentPack);
                System.out.println("Successfully added agent resources to ResourceManager");
            }
        } catch (Exception e) {
            System.err.println("Failed to add agent resources to ResourceManager: " + e.getMessage());
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
        mc.getTextureManager().bindTexture(VAPELOGO_TEXTURE);
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