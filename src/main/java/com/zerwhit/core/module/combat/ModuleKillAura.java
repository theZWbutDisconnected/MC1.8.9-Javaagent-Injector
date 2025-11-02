package com.zerwhit.core.module.combat;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class ModuleKillAura extends ModuleBase implements ITickableModule {
    private long lastAttackTime = 0;

    public ModuleKillAura() {
        super("KillAura", true, "Combat");
        addConfig("Range", 4.0);
        addConfig("Delay", 500);
        addConfig("Players", true);
        addConfig("Mobs", false);
        addConfig("Mode", "Normal");
    }

    @Override
    public void onModuleTick() {
        double range = (Double) getConfig("Range");
        int delay = (Integer) getConfig("Delay");
        boolean attackPlayers = (Boolean) getConfig("Players");
        boolean attackMobs = (Boolean) getConfig("Mobs");
        String mode = (String) getConfig("Mode");

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime < delay) return;

        Entity target = findTarget(range, attackPlayers, attackMobs);
        if (target != null && isValidTarget(target, range)) {
            if (!target.isEntityAlive() || target.hurtResistantTime > 15 || target.isDead) return;

            if (mc.thePlayer.getDistanceToEntity(target) > range + 0.5) return;
            switch (mode) {
                case "Normal":
                case "Silent":
                    if (mc.playerController != null && mc.thePlayer != null) {
                        mc.playerController.attackEntity(mc.thePlayer, target);
                        mc.thePlayer.swingItem();
                    }
                    break;
            }
            lastAttackTime = currentTime;
        }
    }

    private Entity findTarget(double range, boolean players, boolean mobs) {
        Entity closest = null;
        double closestDistance = range + 1;

        List<Entity> entities;
        try {
            entities = mc.theWorld.loadedEntityList.stream().collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }

        for (Entity entity : entities) {
            if (!isValidEntity(entity, players, mobs)) continue;

            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance <= range && distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private boolean isValidEntity(Entity entity, boolean players, boolean mobs) {
        if (entity == null || entity.isDead || entity == mc.thePlayer || !entity.isEntityAlive()) {
            return false;
        }

        if (mc.thePlayer.getDistanceToEntity(entity) > 64) {
            return false;
        }

        if (players && entity instanceof EntityPlayer) {
            return true;
        } else if (mobs && entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)) {
            return true;
        }

        return false;
    }

    private boolean isValidTarget(Entity target, double range) {
        if (target == null) return false;

        return !target.isDead &&
                target.isEntityAlive() &&
                target.hurtResistantTime <= 15 &&
                mc.thePlayer.getDistanceToEntity(target) <= range &&
                mc.theWorld.loadedEntityList.contains(target);
    }

    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Normal":
                    setConfig("Mode", "Silent");
                    break;
                case "Silent":
                    setConfig("Mode", "Normal");
                    break;
                default:
                    setConfig("Mode", "Normal");
            }
        }
    }
    
    @Override
    public int getMaxValueForConfig(String key) {
        if ("Delay".equals(key)) {
            return 1000;
        }
        return super.getMaxValueForConfig(key);
    }
    
    @Override
    public double getMaxDoubleValueForConfig(String key) {
        if ("Range".equals(key)) {
            return 6.0;
        } else if ("FOV".equals(key)) {
            return 180.0;
        } else if ("MaxRotationSpeed".equals(key)) {
            return 360.0;
        } else if ("AntiDetectionStrength".equals(key)) {
            return 3.0;
        }
        return super.getMaxDoubleValueForConfig(key);
    }
}