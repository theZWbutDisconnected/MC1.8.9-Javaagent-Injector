package org.zerwhit.core;

import org.zerwhit.core.manager.TextureRegistry;
import org.zerwhit.core.resource.TextureResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public final class Renderer {
    
    private static final class Constants {
        private static final int GL_QUADS = GL11.GL_QUADS;
        private static final int GL_TRIANGLE_FAN = 6;
        private static final int GL_POLYGON = 7;
        private static final int GL_SCISSOR_TEST = GL11.GL_SCISSOR_TEST;
        private static final int GL_SRC_ALPHA = GL11.GL_SRC_ALPHA;
        private static final int GL_ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;
        private static final int GL_BLEND = GL11.GL_BLEND;
        private static final int GL_TEXTURE_2D = GL11.GL_TEXTURE_2D;
        
        // Anti-aliasing constants - using hardcoded values for older LWJGL versions
        private static final int GL_MULTISAMPLE = 0x809D;
        private static final int GL_LINE_SMOOTH = 0x0B20;
        private static final int GL_LINE_SMOOTH_HINT = 0x0C52;
        private static final int GL_NICEST = 0x1102;
        private static final int GL_DONT_CARE = 0x1100;
        private static final int GL_FASTEST = 0x1101;
        private static final int GL_POLYGON_SMOOTH = 0x0B41;
        private static final int GL_POLYGON_SMOOTH_HINT = 0x0C53;
        private static final int GL_COLOR_BUFFER_BIT = 0x00004000;
        private static final int GL_NEAREST = 0x2600;
        private static final int GL_LINE_LOOP = 0x0002;
        private static final int GL_LINES = 0x0001;
        
        private static final int BLEND_SRC = 770;
        private static final int BLEND_DST = 771;
        private static final int BLEND_SRC_ALPHA = 1;
        private static final int BLEND_DST_ALPHA = 0;
        private static final int SHADE_MODEL_SMOOTH = 7425;
        private static final int SHADE_MODEL_FLAT = 7424;
        
        private Constants() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }
    
    private Renderer() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    public static void drawTexture(String textureName, int x, int y) {
        drawTexture(textureName, x, y, 1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    public static void drawTexture(String textureName, int x, int y, float r, float g, float b, float alpha) {
        TextureResource resource = TextureRegistry.getTextureResource(textureName);
        
        if (!isValidTextureResource(resource)) {
            return;
        }
        
        setupTextureRendering(resource, r, g, b, alpha);
        renderTextureQuad(x, y, resource.getWidth(), resource.getHeight());
        cleanupTextureRendering();
    }
    
    private static boolean isValidTextureResource(TextureResource resource) {
        return resource != null && resource.getTextureId() != -1;
    }
    
    private static void setupTextureRendering(TextureResource resource, float r, float g, float b, float alpha) {
        GL11.glBindTexture(Constants.GL_TEXTURE_2D, resource.getTextureId());
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(Constants.GL_SRC_ALPHA, Constants.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(r, g, b, alpha);
    }
    
    private static void renderTextureQuad(int x, int y, int width, int height) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        double scaleFactor = scaledResolution.getScaleFactor();
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(Constants.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        
        double left = x / scaleFactor;
        double right = (x + width) / scaleFactor;
        double top = y / scaleFactor;
        double bottom = (y + height) / scaleFactor;
        
        worldRenderer.pos(left, bottom, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldRenderer.pos(right, bottom, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldRenderer.pos(right, top, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldRenderer.pos(left, top, 0.0D).tex(0.0D, 0.0D).endVertex();
        
        tessellator.draw();
    }
    
    private static void cleanupTextureRendering() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
    
    public static void drawRect(int x, int y, int width, int height, int color) {
        ColorComponents components = extractColorComponents(color);
        
        setupRectRendering(components);
        renderRect(x, y, width, height);
        cleanupRectRendering();
    }
    
    private static ColorComponents extractColorComponents(int color) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        
        return new ColorComponents(red, green, blue, alpha);
    }
    
    private static void setupRectRendering(ColorComponents components) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(Constants.BLEND_SRC, Constants.BLEND_DST, 
                                          Constants.BLEND_SRC_ALPHA, Constants.BLEND_DST_ALPHA);
        GlStateManager.color(components.red, components.green, components.blue, components.alpha);
    }
    
    private static void renderRect(int x, int y, int width, int height) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(Constants.GL_POLYGON, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x, y + height, 0.0D).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).endVertex();
        worldRenderer.pos(x, y, 0.0D).endVertex();
        tessellator.draw();
    }
    
    private static void cleanupRectRendering() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    
    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        drawRect(x + radius, y, width - radius * 2, height, color);
        drawRect(x, y + radius, width, height - radius * 2, color);
        
        drawPartialCircle(x + radius, y + radius, radius, 90, 180, color);
        drawPartialCircle(x + width - radius, y + radius, radius, 0, 90, color);
        drawPartialCircle(x + radius, y + height - radius, radius, -180, -90, color);
        drawPartialCircle(x + width - radius, y + height - radius, radius, -90, 0, color);
    }
    
    public static void drawPartialCircle(int centerX, int centerY, int radius, int startAngle, int endAngle, int color) {
        ColorComponents components = extractColorComponents(color);
        
        setupCircleRendering(components);
        renderPartialCircle(centerX, centerY, radius, startAngle, endAngle);
        cleanupCircleRendering();
    }
    
    private static void setupCircleRendering(ColorComponents components) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(Constants.BLEND_SRC, Constants.BLEND_DST, 
                                          Constants.BLEND_SRC_ALPHA, Constants.BLEND_DST_ALPHA);
        GlStateManager.color(components.red, components.green, components.blue, components.alpha);
    }
    
    private static void renderPartialCircle(int centerX, int centerY, int radius, int startAngle, int endAngle) {
        int segments = Math.max(8, (endAngle - startAngle) / 10);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(Constants.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        worldRenderer.pos(centerX, centerY, 0.0D).endVertex();
        
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * (startAngle + (endAngle - startAngle) * i / (double) segments) / 180.0;
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY - Math.sin(angle) * radius;
            worldRenderer.pos(x, y, 0.0D).endVertex();
        }
        
        tessellator.draw();
    }
    
    private static void cleanupCircleRendering() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    
    public static void drawCircle(int x, int y, int radius, int color) {
        drawPartialCircle(x, y, radius, 0, 360, color);
    }
    
    public static void drawStringWithShadow(String text, int x, int y, int color) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        fontRenderer.drawStringWithShadow(text, x, y, color);
    }
    
    public static void drawGradientRect(int x, int y, int width, int height, int startColor, int endColor) {
        ColorComponents start = extractColorComponents(startColor);
        ColorComponents end = extractColorComponents(endColor);
        
        setupGradientRendering();
        renderGradientRect(x, y, width, height, start, end);
        cleanupGradientRendering();
    }
    
    private static void setupGradientRendering() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(Constants.BLEND_SRC, Constants.BLEND_DST, 
                                          Constants.BLEND_SRC_ALPHA, Constants.BLEND_DST_ALPHA);
        GlStateManager.shadeModel(Constants.SHADE_MODEL_SMOOTH);
    }
    
    private static void renderGradientRect(int x, int y, int width, int height, ColorComponents start, ColorComponents end) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(Constants.GL_POLYGON, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(x + width, y, 0.0D).color(start.red, start.green, start.blue, start.alpha).endVertex();
        worldRenderer.pos(x, y, 0.0D).color(start.red, start.green, start.blue, start.alpha).endVertex();
        worldRenderer.pos(x, y + height, 0.0D).color(end.red, end.green, end.blue, end.alpha).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).color(end.red, end.green, end.blue, end.alpha).endVertex();
        tessellator.draw();
    }
    
    private static void cleanupGradientRendering() {
        GlStateManager.shadeModel(Constants.SHADE_MODEL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    
    public static void pushScissor(int x, int y, int width, int height) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaledResolution = new ScaledResolution(minecraft);
        int scaleFactor = scaledResolution.getScaleFactor();
        
        GL11.glEnable(Constants.GL_SCISSOR_TEST);
        GL11.glScissor(x * scaleFactor, minecraft.displayHeight - (y + height) * scaleFactor, 
                      width * scaleFactor, height * scaleFactor);
    }
    
    public static void popScissor() {
        GL11.glDisable(Constants.GL_SCISSOR_TEST);
    }
    
    public static void enableMSAA(int samples) {
        if (samples < 2) {
            disableMSAA();
            return;
        }
        
        GL11.glEnable(Constants.GL_MULTISAMPLE);
    }
    
    public static void disableMSAA() {
        GL11.glDisable(Constants.GL_MULTISAMPLE);
    }
    
    public static void enableLineSmoothing() {
        GL11.glEnable(Constants.GL_BLEND);
        GL11.glBlendFunc(Constants.GL_SRC_ALPHA, Constants.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(Constants.GL_LINE_SMOOTH);
        GL11.glHint(Constants.GL_LINE_SMOOTH_HINT, Constants.GL_NICEST);
    }
    
    public static void disableLineSmoothing() {
        GL11.glDisable(Constants.GL_LINE_SMOOTH);
        GL11.glDisable(Constants.GL_BLEND);
    }
    
    public static void setLineSmoothingHint(int quality) {
        if (quality == Constants.GL_NICEST || quality == Constants.GL_FASTEST || quality == Constants.GL_DONT_CARE) {
            GL11.glHint(Constants.GL_LINE_SMOOTH_HINT, quality);
        }
    }
    
    public static void enableAntiAliasing(int msaaSamples, boolean enableLineSmooth) {
        if (msaaSamples >= 2) {
            enableMSAA(msaaSamples);
        }
        
        if (enableLineSmooth) {
            enableLineSmoothing();
        }
    }
    
    public static void disableAntiAliasing() {
        disableMSAA();
        disableLineSmoothing();
    }
    
    public static boolean isMSAAEnabled() {
        return GL11.glIsEnabled(Constants.GL_MULTISAMPLE);
    }
    
    public static boolean isLineSmoothingEnabled() {
        return GL11.glIsEnabled(Constants.GL_LINE_SMOOTH);
    }
    
    public static void drawAntiAliasedLine(float x1, float y1, float x2, float y2, float width, int color) {
        boolean wasLineSmoothingEnabled = isLineSmoothingEnabled();
        boolean wasBlendEnabled = GL11.glIsEnabled(Constants.GL_BLEND);
        if (!wasLineSmoothingEnabled) {
            enableLineSmoothing();
        }
        GL11.glLineWidth(width);
        ColorComponents components = extractColorComponents(color);
        setupRectRendering(components);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(Constants.GL_LINES, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x1, y1, 0.0D).endVertex();
        worldRenderer.pos(x2, y2, 0.0D).endVertex();
        tessellator.draw();
        
        cleanupRectRendering();
        
        // Restore previous state
        GL11.glLineWidth(1.0f);
        
        if (!wasLineSmoothingEnabled) {
            disableLineSmoothing();
        }
        
        if (!wasBlendEnabled) {
            GL11.glDisable(Constants.GL_BLEND);
        }
    }
    
    public static void drawAntiAliasedPolygonOutline(float[] vertices, float width, int color) {
        if (vertices.length < 4 || vertices.length % 2 != 0) {
            return;
        }
        
        boolean wasLineSmoothingEnabled = isLineSmoothingEnabled();
        boolean wasBlendEnabled = GL11.glIsEnabled(Constants.GL_BLEND);
        
        if (!wasLineSmoothingEnabled) {
            enableLineSmoothing();
        }
        GL11.glLineWidth(width);
        
        ColorComponents components = extractColorComponents(color);
        setupRectRendering(components);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(Constants.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        
        for (int i = 0; i < vertices.length; i += 2) {
            worldRenderer.pos(vertices[i], vertices[i + 1], 0.0D).endVertex();
        }
        
        tessellator.draw();
        cleanupRectRendering();
        
        GL11.glLineWidth(1.0f);
        
        if (!wasLineSmoothingEnabled) {
            disableLineSmoothing();
        }
        
        if (!wasBlendEnabled) {
            GL11.glDisable(Constants.GL_BLEND);
        }
    }
    
    public static void enablePolygonSmoothing() {
        GL11.glEnable(Constants.GL_POLYGON_SMOOTH);
        GL11.glHint(Constants.GL_POLYGON_SMOOTH_HINT, Constants.GL_NICEST);
    }
    
    public static void disablePolygonSmoothing() {
        GL11.glDisable(Constants.GL_POLYGON_SMOOTH);
    }
    
    public static void drawAntiAliasedCircle(int centerX, int centerY, int radius, int color, float width) {
        boolean wasLineSmoothingEnabled = isLineSmoothingEnabled();
        boolean wasBlendEnabled = GL11.glIsEnabled(Constants.GL_BLEND);
        
        if (!wasLineSmoothingEnabled) {
            enableLineSmoothing();
        }
        GL11.glLineWidth(width);
        
        ColorComponents components = extractColorComponents(color);
        setupCircleRendering(components);
        
        int segments = Math.max(16, radius * 2);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        worldRenderer.begin(Constants.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(angle) * radius;
            worldRenderer.pos(x, y, 0.0D).endVertex();
        }
        
        tessellator.draw();
        cleanupCircleRendering();
        
        GL11.glLineWidth(1.0f);
        
        if (!wasLineSmoothingEnabled) {
            disableLineSmoothing();
        }
        
        if (!wasBlendEnabled) {
            GL11.glDisable(Constants.GL_BLEND);
        }
    }
    
    private static final class ColorComponents {
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;
        
        private ColorComponents(float red, float green, float blue, float alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }
    }

    public static void drawRoundedRectOutline(int x, int y, int width, int height, int radius, int thickness, int color) {
        drawPartialCircleOutline(x + radius, y + radius, radius, 90, 180, thickness, color);
        drawPartialCircleOutline(x + width - radius, y + radius, radius, 0, 90, thickness, color);
        drawPartialCircleOutline(x + radius, y + height - radius, radius, -180, -90, thickness, color);
        drawPartialCircleOutline(x + width - radius, y + height - radius, radius, -90, 0, thickness, color);

        drawRect(x + radius, y, width - radius * 2, thickness, color); // 上边框
        drawRect(x + radius, y + height - thickness, width - radius * 2, thickness, color); // 下边框
        drawRect(x, y + radius, thickness, height - radius * 2, color); // 左边框
        drawRect(x + width - thickness, y + radius, thickness, height - radius * 2, color); // 右边框
    }
    
    private static void drawPartialCircleOutline(int centerX, int centerY, int radius, int startAngle, int endAngle, int thickness, int color) {
        ColorComponents components = extractColorComponents(color);
        
        setupCircleRendering(components);
        renderPartialCircleOutline(centerX, centerY, radius, startAngle, endAngle, thickness);
        cleanupCircleRendering();
    }
    
    private static void renderPartialCircleOutline(int centerX, int centerY, int radius, int startAngle, int endAngle, int thickness) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        
        int segments = Math.max(16, radius);
        float angleStep = (endAngle - startAngle) / (float) segments;
        
        worldRenderer.begin(Constants.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        
        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(startAngle + angleStep * i);
            float outerX = (float) (centerX + Math.cos(angle) * radius);
            float outerY = (float) (centerY + Math.sin(angle) * radius);
            float innerX = (float) (centerX + Math.cos(angle) * (radius - thickness));
            float innerY = (float) (centerY + Math.sin(angle) * (radius - thickness));
            
            worldRenderer.pos(outerX, outerY, 0.0D).endVertex();
            worldRenderer.pos(innerX, innerY, 0.0D).endVertex();
        }
        
        tessellator.draw();
    }
    
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}