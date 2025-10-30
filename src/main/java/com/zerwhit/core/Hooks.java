package com.zerwhit.core;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.zerwhit.AgentMain;
import com.zerwhit.Main;
import com.zerwhit.core.jni.GraphicLib;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Hooks {
    public static int VAPELOGO_TEXTURE_ID = -1;
    public static int V4_TEXTURE_ID = -1;

    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();

        try {
            if (VAPELOGO_TEXTURE_ID == -1) {
                loadVapelogoTexture();
                loadV4logoTexture();
            }

            drawVapelogo(mc.displayWidth, mc.displayHeight);
            drawV4logo(mc.displayWidth, mc.displayHeight);
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

    private static void loadVapelogoTexture() {
        try {
            // 使用AgentMain的类加载器获取资源流
            InputStream inputStream = AgentMain.class.getClassLoader().getResourceAsStream("assets/zerwhit/textures/vapelogo.png");
            if (inputStream == null) {
                System.err.println("Failed to find vapelogo texture");
                return;
            }

            // 使用ImageIO读取图片
            BufferedImage image = ImageIO.read(inputStream);
            inputStream.close();

            if (image == null) {
                System.err.println("Failed to read image data");
                return;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // 将图像数据转换为ByteBuffer
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    // 转换为RGBA格式
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            // 生成OpenGL纹理
            VAPELOGO_TEXTURE_ID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, VAPELOGO_TEXTURE_ID);

            // 设置纹理参数
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            // 上传纹理数据
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            System.out.println("Successfully loaded vapelogo texture with ID: " + VAPELOGO_TEXTURE_ID);

        } catch (Exception e) {
            System.err.println("Failed to load vapelogo texture: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void loadV4logoTexture() {
        try {
            // 使用AgentMain的类加载器获取资源流
            InputStream inputStream = AgentMain.class.getClassLoader().getResourceAsStream("assets/zerwhit/textures/v4.png");
            if (inputStream == null) {
                System.err.println("Failed to find v4logo texture");
                return;
            }

            // 使用ImageIO读取图片
            BufferedImage image = ImageIO.read(inputStream);
            inputStream.close();

            if (image == null) {
                System.err.println("Failed to read image data");
                return;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // 将图像数据转换为ByteBuffer
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    // 转换为RGBA格式
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            // 生成OpenGL纹理
            V4_TEXTURE_ID = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, V4_TEXTURE_ID);

            // 设置纹理参数
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            // 上传纹理数据
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            System.out.println("Successfully loaded v4logo texture with ID: " + V4_TEXTURE_ID);

        } catch (Exception e) {
            System.err.println("Failed to load v4logo texture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void drawVapelogo(int screenWidth, int screenHeight) {
        if (VAPELOGO_TEXTURE_ID == -1) {
            return; // 纹理未加载
        }

        // 直接使用GL11绑定纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, VAPELOGO_TEXTURE_ID);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int logoWidth = 178;
        int logoHeight = 53;
        int margin = 10;
//        int x = screenWidth - logoWidth - margin;
        int x = margin;
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

    private static void drawV4logo(int screenWidth, int screenHeight) {
        if (V4_TEXTURE_ID == -1) {
            return; // 纹理未加载
        }

        // 直接使用GL11绑定纹理
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, V4_TEXTURE_ID);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int logoWidth = 76;
        int logoHeight = 53;
        int margin = 10;
//        int x = screenWidth - logoWidth - margin;
        int x = 178 + margin * 2;
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

    static {
        System.out.println("Hooks class initialized by classloader: " + Hooks.class.getClassLoader());
    }
}