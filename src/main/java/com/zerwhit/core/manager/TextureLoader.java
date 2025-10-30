package com.zerwhit.core.manager;

import com.zerwhit.AgentMain;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TextureLoader {
    public static int loadTextureFromResource(String resourcePath, String textureName) {
        try {
            InputStream inputStream = AgentMain.class.getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                System.err.println("Failed to find texture: " + resourcePath);
                return -1;
            }

            BufferedImage image = ImageIO.read(inputStream);
            inputStream.close();

            if (image == null) {
                System.err.println("Failed to read image data: " + resourcePath);
                return -1;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }
            buffer.flip();

            int textureId = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            System.out.println("Successfully loaded texture: " + textureName + " with ID: " + textureId);
            return textureId;

        } catch (Exception e) {
            System.err.println("Failed to load texture " + textureName + ": " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    public static void releaseTexture(int textureId) {
        if (textureId != -1) {
            GL11.glDeleteTextures(textureId);
        }
    }
}