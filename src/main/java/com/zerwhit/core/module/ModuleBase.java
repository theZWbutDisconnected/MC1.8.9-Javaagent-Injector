package com.zerwhit.core.module;

import com.zerwhit.core.module.combat.ModuleAutoClicker;
import com.zerwhit.core.module.combat.ModuleKillAura;
import com.zerwhit.core.module.combat.ModuleReach;
import com.zerwhit.core.module.movement.*;
import com.zerwhit.core.module.render.ModuleArraylist;
import com.zerwhit.core.module.render.ModuleXRay;
import com.zerwhit.core.module.visual.ModuleFreeLook;
import com.zerwhit.core.module.visual.ModuleLegacyAnim;
import com.zerwhit.core.module.visual.ModulePostProcessing;
import com.zerwhit.core.module.test.ModuleRotationTest;
import com.zerwhit.core.manager.RotationManager;
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
    protected RotationManager rotationManager = RotationManager.getInstance();

    static {
        addModule(new ModuleFly());
        addModule(new ModuleSprint());
        addModule(new ModuleSpeed());
        addModule(new ModuleNoFall());
        addModule(new ModuleScaffold());
        addModule(new ModuleArraylist());
        addModule(new ModuleXRay());
        addModule(new ModuleKillAura());
        addModule(new ModuleAutoClicker());
        addModule(new ModuleReach());
        addModule(new ModuleLegacyAnim());
        addModule(new ModuleFreeLook());
        addModule(new ModulePostProcessing());
        addModule(new ModuleRotationTest());
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
    
    // ========== Rotation Manager 集成方法 ==========
    
    /**
     * 设置目标旋转角度
     * @param yaw 目标偏航角
     * @param pitch 目标俯仰角
     */
    protected void setTargetRotation(float yaw, float pitch) {
        rotationManager.setTargetRotation(yaw, pitch);
    }
    
    /**
     * 设置目标旋转到实体位置
     * @param targetX 目标X坐标
     * @param targetY 目标Y坐标
     * @param targetZ 目标Z坐标
     */
    protected void setTargetRotationToEntity(double targetX, double targetY, double targetZ) {
        rotationManager.setTargetRotationToEntity(targetX, targetY, targetZ);
    }
    
    /**
     * 停止旋转
     */
    protected void stopRotation() {
        rotationManager.stopRotation();
    }
    
    /**
     * 检查是否正在旋转
     * @return 是否正在旋转
     */
    protected boolean isRotating() {
        return rotationManager.isRotating();
    }
    
    /**
     * 获取旋转进度 (0.0 - 1.0)
     * @return 旋转进度
     */
    protected float getRotationProgress() {
        return rotationManager.getRotationProgress();
    }
    
    /**
     * 设置旋转模式
     * @param mode 旋转模式
     */
    protected void setRotationMode(RotationManager.RotationMode mode) {
        rotationManager.setRotationMode(mode);
    }
    
    /**
     * 设置旋转速度
     * @param speed 旋转速度 (度/秒)
     */
    protected void setRotationSpeed(float speed) {
        rotationManager.setRotationSpeed(speed);
    }
    
    /**
     * 设置最大旋转速度
     * @param maxSpeed 最大旋转速度 (度/秒)
     */
    protected void setMaxRotationSpeed(float maxSpeed) {
        rotationManager.setMaxRotationSpeed(maxSpeed);
    }
    
    /**
     * 设置旋转完成阈值
     * @param threshold 阈值角度
     */
    protected void setRotationThreshold(float threshold) {
        rotationManager.setRotationThreshold(threshold);
    }
    
    /**
     * 获取当前旋转配置
     */
    protected RotationManager.RotationMode getRotationMode() { return rotationManager.getRotationMode(); }
    protected float getRotationSpeed() { return rotationManager.getRotationSpeed(); }
    protected float getMaxRotationSpeed() { return rotationManager.getMaxRotationSpeed(); }
    protected float getRotationThreshold() { return rotationManager.getRotationThreshold(); }
    protected float getCurrentYaw() { return rotationManager.getCurrentYaw(); }
    protected float getCurrentPitch() { return rotationManager.getCurrentPitch(); }
    protected float getTargetYaw() { return rotationManager.getTargetYaw(); }
    protected float getTargetPitch() { return rotationManager.getTargetPitch(); }
}