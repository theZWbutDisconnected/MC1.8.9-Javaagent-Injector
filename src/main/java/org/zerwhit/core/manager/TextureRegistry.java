package org.zerwhit.core.manager;

import org.zerwhit.core.resource.TextureResource;
import org.zerwhit.core.util.SafeLogger;

import java.util.HashMap;
import java.util.Map;

public class TextureRegistry {
    private static final SafeLogger logger = SafeLogger.getLogger(TextureRegistry.class);
    
    private static final Map<String, TextureResource> TEXTURE_REGISTRY = new HashMap<>();
    private static boolean initialized = false;

    public static final String VAPELOGO = "vapelogo";
    public static final String CLIENTLOGO = "clientlogo";
    public static final String V4LOGO = "v4logo";
    public static final String CLIENTLOGONOTITLE = "clientlogonotitle";
    public static final String ON = "on";
    public static final String OFF = "off";

    public static void initialize() {
        if (initialized) return;

        registerTexture(VAPELOGO,
                new TextureResource("zerwhit", "textures/vapelogo.png", VAPELOGO, 178, 53));
        registerTexture(CLIENTLOGO,
                new TextureResource("zerwhit", "textures/clientlogo.png", CLIENTLOGO, 178, 53));
        registerTexture(V4LOGO,
                new TextureResource("zerwhit", "textures/v4.png", VAPELOGO, 76, 53));
        registerTexture(CLIENTLOGONOTITLE,
                new TextureResource("zerwhit", "textures/clientlogonotitle.png", CLIENTLOGONOTITLE, 300, 300));
        registerTexture(ON,
                new TextureResource("zerwhit", "textures/on.png", ON, 128, 56));
        registerTexture(OFF,
                new TextureResource("zerwhit", "textures/off.png", OFF, 128, 56));

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