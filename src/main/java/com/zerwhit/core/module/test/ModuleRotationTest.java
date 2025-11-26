package com.zerwhit.core.module.test;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import com.zerwhit.core.manager.RotationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class ModuleRotationTest extends ModuleBase implements ITickableModule {
    
    private Entity targetEntity = null;
    private long lastRotationTime = 0;
    private boolean isLookingAtRandom = false;
    
    public ModuleRotationTest() {
        super("Rotation Test", false, "Visual");
        addConfig("Rotation Mode", "LINEAR");
        addConfig("Rotation Speed", 180.0f);
        addConfig("Target Entity", false);
        addConfig("Random Look", false);
        addConfig("Stop Rotation", false);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        System.out.println("Rotation Test Module Enabled");
        
        setRotationMode(RotationManager.RotationMode.LINEAR);
        setRotationSpeed(180.0f);
        setRotationThreshold(0.5f);
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        System.out.println("Rotation Test Module Disabled");
        stopRotation();
    }
    
    @Override
    public void onModuleTick() {
        if (!enabled) return;
        updateRotationConfig();
        if ((Boolean) getConfig("Target Entity")) {
            handleEntityTargeting();
        }
        if ((Boolean) getConfig("Random Look")) {
            handleRandomLooking();
        }
        if ((Boolean) getConfig("Stop Rotation")) {
            stopRotation();
            setConfig("Stop Rotation", false);
        }
        if (isRotating()) {
            float progress = getRotationProgress() * 100;
            System.out.println("旋转进度: " + String.format("%.1f", progress) + "%");
        }
    }
    
    private void updateRotationConfig() {
        String modeStr = (String) getConfig("Rotation Mode");
        RotationManager.RotationMode mode;
        
        switch (modeStr) {
            case "LINEAR": mode = RotationManager.RotationMode.LINEAR; break;
            case "SMOOTH": mode = RotationManager.RotationMode.SMOOTH; break;
            case "INSTANT": mode = RotationManager.RotationMode.INSTANT; break;
            case "CUSTOM": mode = RotationManager.RotationMode.CUSTOM; break;
            default: mode = RotationManager.RotationMode.LINEAR;
        }
        
        setRotationMode(mode);
        setRotationSpeed((Float) getConfig("Rotation Speed"));
    }
    
    private void handleEntityTargeting() {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        Entity nearestEntity = findNearestEntity();
        
        if (nearestEntity != null && nearestEntity != targetEntity) {
            targetEntity = nearestEntity;
            setTargetRotationToEntity(
                targetEntity.posX,
                targetEntity.posY + targetEntity.getEyeHeight(),
                targetEntity.posZ
            );
            
            System.out.println("锁定目标: " + targetEntity.getName());
        }
    }
    
    private void handleRandomLooking() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastRotationTime > 3000 || !isLookingAtRandom) {
            float randomYaw = (float) (Math.random() * 360 - 180);
            float randomPitch = (float) (Math.random() * 180 - 90);
            
            setTargetRotation(randomYaw, randomPitch);
            
            System.out.println("随机视角: Yaw=" + randomYaw + ", Pitch=" + randomPitch);
            
            lastRotationTime = currentTime;
            isLookingAtRandom = true;
        }
    }
    
    private Entity findNearestEntity() {
        if (mc.theWorld == null || mc.thePlayer == null) return null;
        
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                double distance = mc.thePlayer.getDistanceToEntity(entity);
                if (distance < nearestDistance && distance < 10.0) {
                    nearestDistance = distance;
                    nearest = entity;
                }
            }
        }
        
        return nearest;
    }
    
    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Rotation Mode")) {
            String currentMode = (String) getConfig("Rotation Mode");
            switch (currentMode) {
                case "LINEAR":
                    setConfig("Rotation Mode", "SMOOTH");
                    break;
                case "SMOOTH":
                    setConfig("Rotation Mode", "INSTANT");
                    break;
                case "INSTANT":
                    setConfig("Rotation Mode", "CUSTOM");
                    break;
                case "CUSTOM":
                    setConfig("Rotation Mode", "LINEAR");
                    break;
                default:
                    setConfig("Rotation Mode", "LINEAR");
            }
        }
    }
    
    @Override
    public double getMaxValueForConfig(String key) {
        if ("Rotation Speed".equals(key)) {
            return 360.0;
        }
        return super.getMaxValueForConfig(key);
    }
    
    @Override
    public double getMinValueForConfig(String key) {
        if ("Rotation Speed".equals(key)) {
            return 1.0;
        }
        return super.getMinValueForConfig(key);
    }
}