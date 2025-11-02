package com.zerwhit.core.module;

import com.zerwhit.core.module.combat.ModuleAutoBlock;
import com.zerwhit.core.module.combat.ModuleAutoClicker;
import com.zerwhit.core.module.combat.ModuleKillAura;
import com.zerwhit.core.module.combat.ModuleReach;
import com.zerwhit.core.module.movement.ModuleEdgeSneak;
import com.zerwhit.core.module.movement.ModuleFly;
import com.zerwhit.core.module.movement.ModuleNoFall;
import com.zerwhit.core.module.movement.ModuleSpeed;
import com.zerwhit.core.module.movement.ModuleSprint;
import com.zerwhit.core.module.render.ModuleArraylist;
import com.zerwhit.core.module.render.ModuleXRay;
import com.zerwhit.core.module.visual.ModuleLegacyAnim;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleBase {
    public static final Map<String, List<ModuleBase>> categories = new HashMap<>();
    public String name;
    public boolean enabled;
    public String category;
    public Map<String, Object> config = new HashMap<>();
    public Map<String, Class<?>> configTypes = new HashMap<>();
    public Minecraft mc;

    static {
        addModule(new ModuleFly());
        addModule(new ModuleSprint());
        addModule(new ModuleSpeed());
        addModule(new ModuleNoFall());
        addModule(new ModuleEdgeSneak());
        addModule(new ModuleArraylist());
        addModule(new ModuleXRay());
        addModule(new ModuleKillAura());
        addModule(new ModuleAutoClicker());
        addModule(new ModuleAutoBlock());
        addModule(new ModuleReach());
        addModule(new ModuleLegacyAnim());
    }

    private static void addModule(ModuleBase module) {
        categories.computeIfAbsent(module.category, k -> new ArrayList<>()).add(module);
    }

    public ModuleBase(String name, boolean enabled, String category) {
        this.mc = Minecraft.getMinecraft();
        this.name = name;
        this.enabled = enabled;
        this.category = category;
        if (enabled) {
            onEnable();
        }
    }

    public String getDisplayName() {
        return name;
    }

    public int getCategoryOrder() {
        switch(category) {
            case "Combat": return 0;
            case "Movement": return 1;
            case "Render": return 2;
            case "Visual": return 3;
            default: return 4;
        }
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

    public void onEnable() {}
    public void onDisable() {}
    
    public void cycleStringConfig(String key) {
        Object currentValue = getConfig(key);
        if (currentValue instanceof String) {
        }
    }
    
    public int getMaxValueForConfig(String key) {
        return 100;
    }
    
    public double getMaxDoubleValueForConfig(String key) {
        return 10.0;
    }
}