package org.zerwhit.core.manager;

import org.zerwhit.core.resource.TextureResource;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TextureLoader {
    private static final Logger logger = LogManager.getLogger(TextureLoader.class);
    
    public static int loadTextureFromResource(String resourcePath, String textureName) {
        try {
            InputStream inputStream = TextureLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                logger.error("Failed to find texture: {}", resourcePath);
                return -1;
            }

            BufferedImage image = ImageIO.read(inputStream);
            inputStream.close();

            if (image == null) {
                logger.error("Failed to read image data: {}", resourcePath);
                return -1;
            }

            return uploadTextureToGPU(image, textureName);

        } catch (Exception e) {
            logger.error("Failed to load texture {}: {}", textureName, e.getMessage());
            logger.error("Error details:", e);
            return -1;
        }
    }

    public static boolean loadTextureResource(TextureResource textureResource) {
        if (textureResource == null) {
            logger.error("TextureResource is null");
            return false;
        }

        if (textureResource.isLoaded()) {
            logger.info("Texture already loaded: {}", textureResource.getTextureName());
            return true;
        }

        int textureId = loadTextureFromResource(
                textureResource.getResourcePath(),
                textureResource.getTextureName()
        );

        if (textureId != -1) {
            textureResource.setTextureId(textureId);
            logger.info("Successfully loaded texture resource: {}", textureResource.getTextureName());
            return true;
        } else {
            logger.error("Failed to load texture resource: {}", textureResource.getTextureName());
            return false;
        }
    }

    public static void loadAllTextureResources() {
        String[] textureKeys = TextureRegistry.getRegisteredTextureKeys();
        int successCount = 0;

        for (String key : textureKeys) {
            TextureResource resource = TextureRegistry.getTextureResource(key);
            if (resource != null && loadTextureResource(resource)) {
                successCount++;
            }
        }

        logger.info("Loaded {}/{} texture resources", successCount, textureKeys.length);
    }

    private static int uploadTextureToGPU(BufferedImage image, String textureName) {
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

        logger.info("Successfully uploaded texture to GPU: {} (ID: {})", textureName, textureId);
        return textureId;
    }

    public static void releaseTexture(int textureId) {
        if (textureId != -1) {
            GL11.glDeleteTextures(textureId);
            logger.info("Released texture ID: {}", textureId);
        }
    }

    public static void releaseTextureResource(TextureResource textureResource) {
        if (textureResource != null && textureResource.isLoaded()) {
            releaseTexture(textureResource.getTextureId());
            textureResource.unload();
        }
    }
}