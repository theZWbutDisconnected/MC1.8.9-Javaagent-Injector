package com.zerwhit.core.module.combat;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import com.zerwhit.core.util.ObfuscationReflectionHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ModuleKillAura extends ModuleBase implements ITickableModule {
    private long lastAttackTime = 0;
    private final Random random = new Random();
    private double lastAttackRange = 0.0;
    private int attackPattern = 0;
    private int antiDetectionCounter = 0;
    private long lastRotationTime = 0;
    private float[] lastRotation = new float[2];

    public ModuleKillAura() {
        super("KillAura", true, "Combat");
        addConfig("Range", 4.0);
        addConfig("Delay", 100);
        addConfig("Players", true);
        addConfig("Mobs", false);
        addConfig("Mode", "Normal");
        addConfig("FOV", 94.0);
        addConfig("RandomDelay", true);
        addConfig("RandomRange", true);
        addConfig("CheckLineOfSight", true);
        addConfig("AntiMatrix", true);
        addConfig("RotationSpoof", true);
        addConfig("HitBoxRandomize", true);
        addConfig("MaxRotationSpeed", 180.0);
        addConfig("AntiDetectionInterval", 3);
        addConfig("AntiDetectionStrength", 1.0); // 1.0-3.0，数值越大反检测越强
    }

    @Override
    public void onModuleTick() {
        double baseRange = (Double) getConfig("Range");
        int baseDelay = (Integer) getConfig("Delay");
        boolean attackPlayers = (Boolean) getConfig("Players");
        boolean attackMobs = (Boolean) getConfig("Mobs");
        String mode = (String) getConfig("Mode");
        double fov = (Double) getConfig("FOV");
        boolean randomDelay = (Boolean) getConfig("RandomDelay");
        boolean randomRange = (Boolean) getConfig("RandomRange");
        boolean checkLineOfSight = (Boolean) getConfig("CheckLineOfSight");
        boolean antiMatrix = (Boolean) getConfig("AntiMatrix");
        boolean rotationSpoof = (Boolean) getConfig("RotationSpoof");
        boolean hitBoxRandomize = (Boolean) getConfig("HitBoxRandomize");
        double maxRotationSpeed = (Double) getConfig("MaxRotationSpeed");
        int antiDetectionInterval = (Integer) getConfig("AntiDetectionInterval");
        double antiDetectionStrength = (Double) getConfig("AntiDetectionStrength");

        double range = randomRange ? baseRange + (random.nextDouble() * 0.6 - 0.3) : baseRange;
        int delay = randomDelay ? baseDelay + random.nextInt(100) - 50 : baseDelay;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime < Math.max(delay, 40)) return;

        Entity target = findTarget(range, attackPlayers, attackMobs);
        if (target != null && isValidTarget(target, range)) {
            if (!isInFieldOfView(target, fov)) {
                return;
            }
            if (checkLineOfSight && !hasLineOfSight(target)) {
                return;
            }
            
            if (!target.isEntityAlive() || target.hurtResistantTime > 15 || target.isDead) return;

            double currentDistance = mc.thePlayer.getDistanceToEntity(target);
            if (currentDistance > range + 0.5) return;
            attackPattern = (attackPattern + 1) % 3;
            
            // 反Matrix检测逻辑
            if (antiMatrix) {
                antiDetectionCounter++;
                
                // 根据反检测强度调整操作频率
                double strengthFactor = Math.max(0.5, Math.min(3.0, antiDetectionStrength));
                
                // 定期发送虚假视角包（根据强度调整频率）
                int adjustedInterval = (int)(antiDetectionInterval / strengthFactor);
                if (antiDetectionCounter >= Math.max(1, adjustedInterval + random.nextInt(3) - 1)) {
                    sendFakeRotationPackets();
                    antiDetectionCounter = 0;
                }
                
                // 视角欺骗（根据强度调整频率）
                long rotationDelay = (long)(50 / strengthFactor + random.nextInt(30));
                if (rotationSpoof && currentTime - lastRotationTime > rotationDelay) {
                    spoofRotation(target, maxRotationSpeed * strengthFactor);
                    lastRotationTime = currentTime;
                }
                
                // HITBox随机化（根据强度调整频率）
                double hitboxChance = 0.3 + (strengthFactor - 1.0) * 0.35; // 1.0时30%，3.0时100%
                if (hitBoxRandomize && random.nextDouble() < hitboxChance) {
                    target = randomizeHitBox(target);
                }
            }
            
            switch (mode) {
                case "Normal":
                case "Silent":
                    if (mc.playerController != null && mc.thePlayer != null) {
                        if (attackPattern == 0 && random.nextDouble() < 0.3) {
                            return;
                        }
                        
                        // 攻击前发送视角包
                        if (antiMatrix && rotationSpoof) {
                            sendAttackRotation(target);
                        }
                        
                        mc.playerController.attackEntity(mc.thePlayer, target);
                        mc.thePlayer.swingItem();
                        lastAttackRange = currentDistance;
                    }
                    break;
            }
            lastAttackTime = currentTime;
        }
    }

    private Entity findTarget(double range, boolean players, boolean mobs) {
        Entity bestTarget = null;
        double bestScore = -1.0;

        List<Entity> entities;
        try {
            entities = mc.theWorld.loadedEntityList.stream().collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }

        java.util.Collections.shuffle(entities, random);

        for (Entity entity : entities) {
            if (!isValidEntity(entity, players, mobs)) continue;

            double distance = mc.thePlayer.getDistanceToEntity(entity);
            if (distance > range) continue;
            double score = calculateTargetScore(entity, distance);
            if (random.nextDouble() < 0.1) {
                score *= 0.5;
            }

            if (score > bestScore) {
                bestTarget = entity;
                bestScore = score;
            }
        }
        return bestTarget;
    }

    private double calculateTargetScore(Entity entity, double distance) {
        double score = 0.0;
        score += (1.0 - Math.min(distance / 6.0, 1.0)) * 40.0;
        
        if (mc.thePlayer != null) {
            double dx = entity.posX - mc.thePlayer.posX;
            double dz = entity.posZ - mc.thePlayer.posZ;
            double playerYaw = MathHelper.wrapAngleTo180_double(mc.thePlayer.rotationYaw);
            double targetAngle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
            targetAngle = MathHelper.wrapAngleTo180_double(targetAngle);
            double angleDiff = Math.abs(MathHelper.wrapAngleTo180_double(targetAngle - playerYaw));
            
            score += (1.0 - Math.min(angleDiff / 180.0, 1.0)) * 30.0;
        }
        
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            float healthRatio = living.getHealth() / living.getMaxHealth();
            score += (1.0 - healthRatio) * 20.0;
        }
        
        if (hasLineOfSight(entity)) {
            score += 10.0;
        }
        
        return score;
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

    private boolean isInFieldOfView(Entity target, double fov) {
        if (mc.thePlayer == null || target == null) return false;
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double playerYaw = MathHelper.wrapAngleTo180_double(mc.thePlayer.rotationYaw);
        double targetAngle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        targetAngle = MathHelper.wrapAngleTo180_double(targetAngle);
        double angleDiff = Math.abs(MathHelper.wrapAngleTo180_double(targetAngle - playerYaw));
        return angleDiff <= fov / 2.0;
    }

    private boolean hasLineOfSight(Entity target) {
        if (mc.thePlayer == null || target == null) return false;
        Vec3 playerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 targetPos = new Vec3(target.posX, target.posY + target.getEyeHeight(), target.posZ);
        return mc.theWorld.rayTraceBlocks(playerPos, targetPos) == null;
    }

    // 反Matrix检测方法
    private void sendFakeRotationPackets() {
        if (mc.thePlayer == null) return;
        
        // 添加随机延迟，避免固定时间间隔被检测
        try {
            Thread.sleep(random.nextInt(5) + 1); // 1-5ms随机延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 发送随机视角包来干扰检测（减少包数量，增加随机性）
        int packetCount = random.nextBoolean() ? 1 : 2; // 随机选择1或2个包
        for (int i = 0; i < packetCount; i++) {
            // 更小的随机角度变化，模拟自然视角移动
            float yaw = mc.thePlayer.rotationYaw + (random.nextFloat() * 20 - 10);
            float pitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + (random.nextFloat() * 15 - 7.5f), -90, 90);
            
            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
                yaw, pitch, mc.thePlayer.onGround
            ));
            
            // 包之间添加随机延迟
            if (i < packetCount - 1) {
                try {
                    Thread.sleep(random.nextInt(3) + 1); // 1-3ms随机延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // 只在50%的情况下发送恢复包，增加随机性
        if (random.nextBoolean()) {
            // 恢复原始视角前添加延迟
            try {
                Thread.sleep(random.nextInt(3) + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
                mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, mc.thePlayer.onGround
            ));
        }
    }

    private void spoofRotation(Entity target, double maxRotationSpeed) {
        if (mc.thePlayer == null || target == null) return;
        
        // 计算到目标的真实角度
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        targetYaw = MathHelper.wrapAngleTo180_double(targetYaw);
        
        double targetPitch = -Math.toDegrees(Math.atan2(
            target.posY + target.getEyeHeight() - mc.thePlayer.posY - mc.thePlayer.getEyeHeight(),
            Math.sqrt(dx * dx + dz * dz)
        ));
        
        // 限制旋转速度，模拟人类操作
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        
        float yawDiff = MathHelper.wrapAngleTo180_float((float)targetYaw - currentYaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float((float)targetPitch - currentPitch);
        
        // 限制最大旋转速度
        float maxRotation = (float) maxRotationSpeed / 20f; // 每tick最大旋转角度
        yawDiff = MathHelper.clamp_float(yawDiff, -maxRotation, maxRotation);
        pitchDiff = MathHelper.clamp_float(pitchDiff, -maxRotation, maxRotation);
        
        // 应用平滑旋转
        float newYaw = currentYaw + yawDiff;
        float newPitch = MathHelper.clamp_float(currentPitch + pitchDiff, -90, 90);
        
        // 保存最后旋转角度用于反检测
        lastRotation[0] = newYaw;
        lastRotation[1] = newPitch;
    }

    private Entity randomizeHitBox(Entity target) {
        if (target == null || !(target instanceof EntityLivingBase)) return target;
        
        // 轻微随机化HITBox位置来避免检测
        EntityLivingBase living = (EntityLivingBase) target;
        
        // 保存原始位置
        double originalX = living.posX;
        double originalY = living.posY;
        double originalZ = living.posZ;
        
        // 应用随机偏移（很小，避免明显异常）
        living.posX += (random.nextDouble() * 0.1 - 0.05);
        living.posY += (random.nextDouble() * 0.05 - 0.025);
        living.posZ += (random.nextDouble() * 0.1 - 0.05);
        
        // 创建临时实体用于攻击
        Entity tempEntity = createTemporaryEntity(living);
        
        // 恢复原始位置
        living.posX = originalX;
        living.posY = originalY;
        living.posZ = originalZ;
        
        return tempEntity != null ? tempEntity : target;
    }

    private Entity createTemporaryEntity(EntityLivingBase original) {
        if (original == null || mc.theWorld == null) return original;
        
        try {
            EntityLivingBase tempEntity = null;
            
            if (original instanceof EntityPlayer) {
                EntityPlayer originalPlayer = (EntityPlayer) original;
                tempEntity = new EntityPlayer(mc.theWorld, originalPlayer.getGameProfile()) {
                    @Override
                    public boolean isSpectator() { return false; }
                    @Override
                    public void addChatMessage(net.minecraft.util.IChatComponent component) {}
                    @Override
                    public boolean canCommandSenderUseCommand(int permLevel, String commandName) { return false; }
                };
            } else {
                try {
                    tempEntity = (EntityLivingBase) original.getClass()
                        .getConstructor(mc.theWorld.getClass())
                        .newInstance(mc.theWorld);
                } catch (Exception e) {
                    tempEntity = new EntityLivingBase(mc.theWorld) {
                        @Override
                        public net.minecraft.item.ItemStack getHeldItem() { return null; }
                        @Override
                        public net.minecraft.item.ItemStack getEquipmentInSlot(int slotIn) { return null; }
                        @Override
                        public void setCurrentItemOrArmor(int slotIn, net.minecraft.item.ItemStack stack) {}
                        @Override
                        public net.minecraft.item.ItemStack[] getInventory() { return new net.minecraft.item.ItemStack[0]; }
                        @Override
                        public ItemStack getCurrentArmor(int arg0) {
                            return null;
                        }
                    };
                }
            }
            
            if (tempEntity != null) {
                tempEntity.setPosition(original.posX, original.posY, original.posZ);
                tempEntity.prevPosX = original.prevPosX;
                tempEntity.prevPosY = original.prevPosY;
                tempEntity.prevPosZ = original.prevPosZ;
                tempEntity.setRotationYawHead(original.rotationYawHead);
                tempEntity.rotationYaw = original.rotationYaw;
                tempEntity.rotationPitch = original.rotationPitch;
                tempEntity.prevRotationYaw = original.prevRotationYaw;
                tempEntity.prevRotationPitch = original.prevRotationPitch;
                tempEntity.setHealth(original.getHealth());
                tempEntity.setEntityId(original.getEntityId() + 1000000);
                
                tempEntity.width = original.width;
                tempEntity.height = original.height;
                ObfuscationReflectionHelper.invokeObfuscatedMethod(EntityLivingBase.class, new String[]{"setSize", "func_70105_a"}, original, original.width, original.height);
                
                tempEntity.motionX = original.motionX;
                tempEntity.motionY = original.motionY;
                tempEntity.motionZ = original.motionZ;
                
                double offsetX = (random.nextDouble() * 0.08 - 0.04);
                double offsetY = (random.nextDouble() * 0.04 - 0.02);
                double offsetZ = (random.nextDouble() * 0.08 - 0.04);
                
                tempEntity.setPosition(
                    original.posX + offsetX,
                    original.posY + offsetY,
                    original.posZ + offsetZ
                );
                
                ObfuscationReflectionHelper.invokeObfuscatedMethod(EntityLivingBase.class, new String[]{"setDead", "func_70106_y"}, tempEntity, false);
                
                return tempEntity;
            }
        } catch (Exception e) {
            System.err.println("Failed to create temporary entity: " + e.getMessage());
            e.printStackTrace();
        }
        return original;
    }

    private void sendAttackRotation(Entity target) {
        if (mc.thePlayer == null || target == null) return;
        
        // 只在70%的情况下发送攻击前视角包，增加随机性
        if (random.nextDouble() > 0.7) return;
        
        // 添加随机延迟，避免固定时间间隔
        try {
            Thread.sleep(random.nextInt(8) + 2); // 2-9ms随机延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        targetYaw = MathHelper.wrapAngleTo180_double(targetYaw);
        
        double targetPitch = -Math.toDegrees(Math.atan2(
            target.posY + target.getEyeHeight() - mc.thePlayer.posY - mc.thePlayer.getEyeHeight(),
            Math.sqrt(dx * dx + dz * dz)
        ));
        
        // 更小的随机性，避免过大角度变化
        targetYaw += (random.nextDouble() * 6 - 3);
        targetPitch += (random.nextDouble() * 3 - 1.5);
        
        // 发送视角包
        mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
            (float)targetYaw, (float)targetPitch, mc.thePlayer.onGround
        ));
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