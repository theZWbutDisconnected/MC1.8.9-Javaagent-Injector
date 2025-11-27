package com.zerwhit.core.module.combat;

import com.zerwhit.core.Meta;
import com.zerwhit.core.manager.RotationManager;
import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import com.zerwhit.core.util.ObfuscationReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.Timer;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ModuleKillAura extends ModuleBase implements ITickableModule {
    private long lastAttackTime = 0;

    public ModuleKillAura() {
        super("KillAura", true, "Combat");
        addConfig("Range", 4.0);
        addConfig("Delay", 100);
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
        if (currentTime - lastAttackTime < Math.max(50, delay)) return;

        Entity target = findTarget(range, attackPlayers, attackMobs);
        if (isValidTarget(target, range)) {
            setRotationSpeed(720.0F);
            setRotationMode(RotationManager.RotationMode.SMOOTH);
            setRotationThreshold(0.5F);
            Meta.slientAimEnabled = true;
            rotationManager.setTargetRotationToPos(target.posX, target.posY + new Random().nextFloat(), target.posZ);
            if (!target.isEntityAlive() || target.hurtResistantTime > 15 || target.isDead) return;

            double dis = mc.thePlayer.getDistanceToEntity(target);
            Timer timer = (Timer) ObfuscationReflectionHelper.getObfuscatedFieldValue(Minecraft.class, new String[]{"timer", "field_71428_T"}, Minecraft.getMinecraft());
            float partialTicks = 0;
            if (timer != null) {
                partialTicks = timer.renderPartialTicks;
            }
            if (dis > range + 0.5/* || mc.thePlayer.rayTrace(dis, partialTicks).entityHit != target*/) return;
            switch (mode) {
                case "Normal":
                case "Silent":
                    if (mc.playerController != null && mc.thePlayer != null) {
                        boolean sprint = mc.thePlayer.isSprinting();
                        if (sprint) {
                            mc.thePlayer.setSprinting(false);
                            mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                        }
                        mc.thePlayer.swingItem();
                        mc.playerController.attackEntity(mc.thePlayer, target);
                        if (sprint) {
                            mc.thePlayer.setSprinting(true);
                            mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                        }
                    }
                    break;
            }
            lastAttackTime = currentTime;
        }
    }

    @Override
    public void onDisable() {
        Meta.slientAimEnabled = false;
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
    public double getMaxValueForConfig(String key) {
        if ("Range".equals(key)) {
            return 6.0;
        } else if ("FOV".equals(key)) {
            return 180.0;
        } else if ("MaxRotationSpeed".equals(key)) {
            return 360.0;
        } else if ("AntiDetectionStrength".equals(key)) {
            return 3.0;
        }else if ("Delay".equals(key)) {
            return 1000.0;
        }
        return super.getMaxValueForConfig(key);
    }
}