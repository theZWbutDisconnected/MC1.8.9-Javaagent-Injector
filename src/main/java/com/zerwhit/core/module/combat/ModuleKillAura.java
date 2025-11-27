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
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.Timer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ModuleKillAura extends ModuleBase implements ITickableModule {
    private long lastAttackTime = 0;
    private boolean hasTarget;
    private Random random = new Random();
    private long lastSprintToggle = 0;
    private int missCounter = 0;
    private long lastHumanBehavior = 0;
    private boolean isAttacking = false;

    public ModuleKillAura() {
        super("KillAura", true, "Combat");
        addConfig("Range", 3.5);
        addConfig("Delay", 150);
        addConfig("Players", true);
        addConfig("Mobs", false);
        addConfig("RequireMouseDown", true);
        addConfig("Mode", "Normal");
        addConfig("RandomDelay", true);
        addConfig("RandomAngle", true);
        addConfig("SprintFix", true);
        addConfig("AttackBurst", true);
    }

    @Override
    public void onModuleTick() {
        double range = (Double) getConfig("Range");
        int delay = (Integer) getConfig("Delay");
        boolean attackPlayers = (Boolean) getConfig("Players");
        boolean attackMobs = (Boolean) getConfig("Mobs");
        boolean mouseDown = (Boolean) getConfig("RequireMouseDown");
        String mode = (String) getConfig("Mode");
        boolean randomDelay = (Boolean) getConfig("RandomDelay");
        boolean randomAngle = (Boolean) getConfig("RandomAngle");
        boolean sprintFix = (Boolean) getConfig("SprintFix");
        boolean attackBurst = (Boolean) getConfig("AttackBurst");

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastHumanBehavior > 2000 + random.nextInt(3000)) {
            if (random.nextBoolean()) {
                lastHumanBehavior = currentTime;
                return;
            }
            lastHumanBehavior = currentTime;
        }

        int actualDelay = delay;
        if (randomDelay) {
            actualDelay = delay + random.nextInt(41) - 20;
        }
        
        if (currentTime - lastAttackTime < Math.max(80, actualDelay)) return;

        Entity target = findTarget(range, attackPlayers, attackMobs);
        boolean accept = false;
        Label : {
            if (mouseDown && !Mouse.isButtonDown(0)) break Label;
            if (isValidTarget(target, range)) {
                double dis = mc.thePlayer.getDistanceToEntity(target);
                
                hasTarget = true;
                setRotationSpeed(720.0F);
                setRotationMode(RotationManager.RotationMode.CUSTOM);
                setRotationThreshold(0.2F);
                Meta.slientAimEnabled = true;
                if (dis > range + 0.5 || !canSeeHitbox(target)) break Label;

                double yOffset = target.posY + (randomAngle ? (random.nextFloat() * 0.3 - 0.15) : 0);
                rotationManager.setTargetRotationToPos(
                    target.posX + (random.nextFloat() * 0.3 - 0.15),
                    yOffset, 
                    target.posZ + (random.nextFloat() * 0.3 - 0.15)
                );
                if (!target.isEntityAlive() || ((EntityLivingBase) target).hurtTime > 8 || target.isDead) break Label;

                accept = true;
                
                boolean shouldMiss = random.nextInt(100) < 5;
                if (shouldMiss) {
                    missCounter++;
                    if (missCounter > 2) {
                        missCounter = 0;
                        break Label;
                    }
                } else {
                    missCounter = 0;
                }
                
                switch (mode) {
                    case "Normal":
                    case "Silent":
                        if (mc.playerController != null && mc.thePlayer != null) {
                            boolean sprint = mc.thePlayer.isSprinting();
                            boolean block = mc.thePlayer.isBlocking();
                            if (block) {
                                mc.thePlayer.stopUsingItem();
                            }
                            if (sprint && sprintFix) {
                                if (currentTime - lastSprintToggle > 150 + random.nextInt(100)) {
                                    mc.thePlayer.setSprinting(false);
                                    mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
                                    lastSprintToggle = currentTime;
                                }
                            }
                            if (attackBurst && isAttacking && random.nextInt(100) < 30) {
                                isAttacking = false;
                                break Label;
                            }
                            isAttacking = true;
                            mc.thePlayer.swingItem();
                            mc.playerController.attackEntity(mc.thePlayer, target);
                            if (sprint && sprintFix && currentTime - lastSprintToggle > 80 + random.nextInt(70)) {
                                mc.thePlayer.setSprinting(true);
                                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                                lastSprintToggle = currentTime;
                            }
                        }
                        break;
                }

                if (randomDelay) {
                    lastAttackTime = currentTime + random.nextInt(21) - 10;
                } else {
                    lastAttackTime = currentTime;
                }
            }
        }
        if (!accept){
            if (hasTarget)
                Meta.slientAimEnabled = false;
            hasTarget = false;
            isAttacking = false;
        }
    }

    @Override
    public void onDisable() {
        Meta.slientAimEnabled = false;
        isAttacking = false;
    }

    private boolean canSeeHitbox(Entity target) {
        if (target == null || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }

        Vec3 playerEyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 targetPos = new Vec3(target.posX, target.posY + target.height / 2, target.posZ);

        MovingObjectPosition rayTraceResult = mc.theWorld.rayTraceBlocks(
            playerEyes,
            targetPos
        );

        return rayTraceResult == null || rayTraceResult.entityHit == target;
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
                target instanceof EntityLivingBase &&
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
        } else if ("MissChance".equals(key)) {
            return 20.0;
        }
        return super.getMaxValueForConfig(key);
    }
}