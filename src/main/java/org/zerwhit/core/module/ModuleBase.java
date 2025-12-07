package org.zerwhit.core.module;

import org.zerwhit.core.module.combat.ModuleAutoClicker;
import org.zerwhit.core.module.combat.ModuleKillAura;
import org.zerwhit.core.module.combat.ModuleReach;
import org.zerwhit.core.module.movement.*;
import org.zerwhit.core.module.player.ModuleScaffold;
import org.zerwhit.core.module.render.ModuleArraylist;
import org.zerwhit.core.module.render.ModuleXRay;
import org.zerwhit.core.module.visual.ModuleFreeLook;
import org.zerwhit.core.module.visual.ModuleLegacyAnim;
import org.zerwhit.core.module.visual.ModulePostProcessing;
import org.zerwhit.core.module.movement.ModuleMovementFix;
import org.zerwhit.core.manager.RotationManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ModuleBase {
    public static final Map<String, List<ModuleBase>> categories = new HashMap<>();
    public String name;
    public boolean enabled;
    public String category;
    public Map<String, Object> config = new HashMap<>();
    public Map<String, Class<?>> configTypes = new HashMap<>();
    public Minecraft mc;
    protected RotationManager rotMng = RotationManager.getInstance();

    static {
        addModule(new ModuleFly());
        addModule(new ModuleSprint());
        addModule(new ModuleSpeed());
        addModule(new ModuleNoFall());
        addModule(new ModuleScaffold());
        addModule(new ModuleMovementFix());
        addModule(new ModuleArraylist());
        addModule(new ModuleXRay());
        addModule(new ModuleKillAura());
        addModule(new ModuleAutoClicker());
        addModule(new ModuleReach());
        addModule(new ModuleLegacyAnim());
        addModule(new ModuleFreeLook());
        addModule(new ModulePostProcessing());
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
    
    public double getMaxValueForConfig(String key) {
        return 10.0;
    }
    
    public double getMinValueForConfig(String key) {
        return 0;
    }

    protected void setTargetRotation(float yaw, float pitch) {
        rotMng.setTargetRotation(yaw, pitch);
    }
    protected void setTargetRotationToEntity(double targetX, double targetY, double targetZ) {
        rotMng.setTargetRotationToPos(targetX, targetY, targetZ);
    }
    protected void stopRotation() {
        rotMng.stopRotation();
    }
    protected boolean isRotating() {
        return rotMng.isRotating();
    }
    protected float getRotationProgress() {
        return rotMng.getRotationProgress();
    }
    protected void setRotationMode(RotationManager.RotationMode mode) {
        rotMng.setRotationMode(mode);
    }
    protected void setRotationSpeed(float speed) {
        rotMng.setRotationSpeed(speed);
    }
    protected void setMaxRotationSpeed(float maxSpeed) {
        rotMng.setMaxRotationSpeed(maxSpeed);
    }
    protected void setRotationThreshold(float threshold) {
        rotMng.setRotationThreshold(threshold);
    }
    protected RotationManager.RotationMode getRotationMode() { return rotMng.getRotationMode(); }
    protected float getRotationSpeed() { return rotMng.getRotationSpeed(); }
    protected float getMaxRotationSpeed() { return rotMng.getMaxRotationSpeed(); }
    protected float getRotationThreshold() { return rotMng.getRotationThreshold(); }
    protected float getCurrentYaw() { return rotMng.getCurrentYaw(); }
    protected float getCurrentPitch() { return rotMng.getCurrentPitch(); }
    protected float getTargetYaw() { return rotMng.getTargetYaw(); }
    protected float getTargetPitch() { return rotMng.getTargetPitch(); }
}