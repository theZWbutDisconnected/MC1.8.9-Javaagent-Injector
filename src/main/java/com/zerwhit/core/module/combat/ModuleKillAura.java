package com.zerwhit.core.module.combat;

import com.zerwhit.core.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class ModuleKillAura extends Module {
    private long lastAttackTime = 0;
    
    public ModuleKillAura() {
        super("KillAura", true, "Combat");
        addConfig("Range", 4.0);
        addConfig("Delay", 500);
        addConfig("Players", true);
        addConfig("Mobs", false);
        addConfig("Mode", "Normal");
        addConfig("Animation", true);
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
        if (target != null && target.hurtResistantTime <= 0) {
            switch (mode) {
                case "Normal":
                    mc.playerController.attackEntity(mc.thePlayer, target);
                    mc.thePlayer.swingItem();
                    break;
                case "Silent":
                    mc.playerController.attackEntity(mc.thePlayer, target);
                    mc.thePlayer.swingItem();
                    break;
            }
            lastAttackTime = currentTime;
        }
    }
    
    private Entity findTarget(double range, boolean players, boolean mobs) {
        Entity closest = null;
        double closestDistance = range + 1;
        
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer || !entity.isEntityAlive()) continue;
            
            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance <= range && distance < closestDistance) {
                boolean validTarget = false;
                if (players && entity instanceof EntityPlayer) {
                    validTarget = true;
                } else if (mobs && !(entity instanceof EntityPlayer)) {
                    validTarget = true;
                }
                
                if (validTarget) {
                    closest = entity;
                    closestDistance = distance;
                }
            }
        }
        return closest;
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
}