package com.zerwhit.core.manager;

import com.zerwhit.core.resource.TextureResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class TextureRegistry {
    private static final Logger logger = LogManager.getLogger(TextureRegistry.class);
    
    private static final Map<String, TextureResource> TEXTURE_REGISTRY = new HashMap<>();
    private static boolean initialized = false;

    public static final String VAPELOGO = "vapelogo";
    public static final String CLIENTLOGO = "clientlogo";
    public static final String V4LOGO = "v4logo";

    public static void initialize() {
        if (initialized) return;

        registerTexture(VAPELOGO,
                new TextureResource("zerwhit", "textures/vapelogo.png", "vapelogo", 178, 53));
        registerTexture(CLIENTLOGO,
                new TextureResource("zerwhit", "textures/clientlogo.png", "clientlogo", 178, 53));
        registerTexture(V4LOGO,
                new TextureResource("zerwhit", "textures/v4.png", "v4logo", 76, 53));

        initialized = true;
        logger.info("TextureRegistry initialized with {} textures", TEXTURE_REGISTRY.size());
    }

    public static void registerTexture(String key, TextureResource textureResource) {
        if (TEXTURE_REGISTRY.containsKey(key)) {
            logger.warn("Texture already registered: {}", key);
            return;
        }
        TEXTURE_REGISTRY.put(key, textureResource);
        logger.info("Registered texture: {} -> {}", key, textureResource.getResourcePath());
    }

    public static TextureResource getTextureResource(String key) {
        if (!TEXTURE_REGISTRY.containsKey(key)) {
            logger.warn("Texture not found: {}", key);
            return null;
        }
        return TEXTURE_REGISTRY.get(key);
    }

    public static boolean isTextureLoaded(String key) {
        TextureResource resource = getTextureResource(key);
        return resource != null && resource.isLoaded();
    }

    public static String[] getRegisteredTextureKeys() {
        return TEXTURE_REGISTRY.keySet().toArray(new String[0]);
    }

    public static void cleanup() {
        for (TextureResource resource : TEXTURE_REGISTRY.values()) {
            if (resource.isLoaded()) {
                TextureLoader.releaseTexture(resource.getTextureId());
                resource.unload();
            }
        }
        TEXTURE_REGISTRY.clear();
        initialized = false;
        logger.info("TextureRegistry cleaned up");
    }
}