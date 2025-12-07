package org.zerwhit.core.resource;

import net.minecraft.util.ResourceLocation;

public class TextureResource {
    private final ResourceLocation resourceLocation;
    private final String resourcePath;
    private final String textureName;
    private int textureId = -1;
    private boolean loaded = false;
    private final int width;
    private final int height;

    public TextureResource(String domain, String path, String textureName, int width, int height) {
        this.resourceLocation = new ResourceLocation(domain, path);
        this.resourcePath = "assets/" + domain + "/" + path;
        this.textureName = textureName;
        this.width = width;
        this.height = height;
    }

    public ResourceLocation getResourceLocation() { return resourceLocation; }
    public String getResourcePath() { return resourcePath; }
    public String getTextureName() { return textureName; }
    public int getTextureId() { return textureId; }
    public boolean isLoaded() { return loaded; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
        this.loaded = textureId != -1;
    }

    public void unload() {
        this.textureId = -1;
        this.loaded = false;
    }
}