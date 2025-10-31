package com.zerwhit.core.module;

import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;

public class Module {
    public String name;
    public boolean enabled;
    public String category;
    public Map<String, Object> config = new HashMap<>();
    public Map<String, Class<?>> configTypes = new HashMap<>();
    public Minecraft mc;

    public Module(String name, boolean enabled, String category) {
        this.mc = Minecraft.getMinecraft();
        this.name = name;
        this.enabled = enabled;
        this.category = category;
    }

    public void addConfig(String key, Object defaultValue) {
        config.put(key, defaultValue);
        configTypes.put(key, defaultValue.getClass());
    }

    public Object getConfig(String key) {
        return config.get(key);
    }

    public void setConfig(String key, Object value) {
        config.put(key, value);
    }

    public void onModuleTick() {}
    public void onEnable() {}
    public void onDisable() {}
    
    public void cycleStringConfig(String key) {
        Object currentValue = getConfig(key);
        if (currentValue instanceof String) {
        }
    }
}