package org.zerwhit.core.module.combat;

import javafx.scene.input.KeyCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.KeyRobot;
import org.zerwhit.core.util.ObfuscationReflectionHelper;

import java.util.List;
import java.util.Random;

public class ModuleSlientAura extends ModuleBase implements ITickableModule {
    private Entity target = null;
    private int lastAttackTick;
    private Random rand = new Random();
    private boolean slient;
    private long clickDelay = 0L;
    private long lastPitchUpdateTime = 0L;
    private float pitchNoiseOffset = 0f;
    private float pitchNoiseVelocity = 0f;
    private float pitchTargetOffset = 0f;
    private float pitchSmoothFactor = 0.15f;

    float newYaw, newPitch;

    public ModuleSlientAura() {
        super("SlientAura", true, "Combat", KeyCode.R);
        addConfig("Distance", 3.0f);
        addConfig("SlientAim", false);
        addConfig("PitchEnabled", true);
        addConfig("AutoBlock", true);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Meta.slientAimEnabled = false;
        this.slient = false;
        Meta.blockRenderEnabled = false;
    }

    @Override
    public void onModuleTick() {
        Float distance = (Float) getConfig("Distance");
        Boolean slient = (Boolean) getConfig("SlientAim");
        Boolean pitchEnabled = (Boolean) getConfig("PitchEnabled");
        Boolean autoBlock = (Boolean) getConfig("AutoBlock");
        Meta.blockRenderEnabled = autoBlock;
        List<Entity> entityList = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().expand(distance, distance, distance));
        if (!entityList.isEmpty()) Meta.slientAimEnabled = slient;
        for (int i = 0; i < entityList.size(); i++) {
            Entity entity = entityList.get(i);
            if (!(entity.isEntityAlive() && !entity.isDead)) return;
            if (target == null || target.getDistanceSqToEntity(mc.thePlayer) > entity.getDistanceSqToEntity(mc.thePlayer)) target = entity;
        }
        
        if (target == null) return;
        
        double deltaX = target.posX - mc.thePlayer.posX;
        double deltaY = (target.posY + 1) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = target.posZ - mc.thePlayer.posZ;
        double dis = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);

        double targetPitch = (float) (-(Math.atan2(deltaY, dis) * 180.0 / Math.PI));
        double targetYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;

        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;

        float yawDiff = (float) (targetYaw - currentYaw);
        yawDiff = rotMng.normalizeAngle(yawDiff);

        boolean shouldReverseYaw = isViewExceedingBoundingBox(currentYaw, target);
        
        if (MathHelper.abs(yawDiff) > 10 + rand.nextFloat() * 5) {
            if (shouldReverseYaw) {
                newYaw = currentYaw - yawDiff * 0.8f;
            } else {
                newYaw = currentYaw + yawDiff;
            }
        }
        
        if (!shouldReverseYaw) {
            mc.thePlayer.rotationYaw = rotMng.normalizeAngleTo360(currentYaw + (newYaw - currentYaw) * 0.08f);
        }
        
        if (pitchEnabled) {
            float pitchDiff = (float) (targetPitch - currentPitch);
            pitchDiff = Math.max(-90, Math.min(90, pitchDiff));

            newPitch = currentPitch + pitchDiff;
            newPitch = Math.max(-90, Math.min(90, newPitch));

            float smoothPitchChange = calculateSmoothPitchChange(currentPitch, newPitch);
            mc.thePlayer.rotationPitch += smoothPitchChange;
        }
        
        this.slient = true;
        handleClick();
    }

    private void handleClick() {
        if (lastAttackTick > 0) { lastAttackTick -= 1; return;}
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
        lastAttackTick = (int) (rand.nextDouble() * 20 + 10);
    }
    
    private boolean isViewExceedingBoundingBox(float currentYaw, Entity target) {
        if (target == null) return false;
        
        double playerX = mc.thePlayer.posX;
        double playerY = mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
        double playerZ = mc.thePlayer.posZ;
        
        double targetX = target.posX;
        double targetY = target.posY + target.getEyeHeight();
        double targetZ = target.posZ;
        
        double deltaX = targetX - playerX;
        double deltaZ = targetZ - playerZ;
        
        double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 0.1) return false;
        
        double targetYaw = Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI - 90.0;
        double yawDiff = rotMng.normalizeAngle((float)(targetYaw - currentYaw));
        
        double boundingBoxWidth = target.width / 2.0;
        double boundingBoxDepth = target.width / 2.0;
        
        double angleThreshold = Math.toDegrees(Math.atan2(boundingBoxWidth, distance)) * 0.7;
        
        return Math.abs(yawDiff) < angleThreshold;
    }
    
    private float calculateSmoothPitchChange(float currentPitch, float targetPitch) {
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastPitchUpdateTime) / 1_000_000_000.0f;
        lastPitchUpdateTime = currentTime;
        if (deltaTime > 0.1f) {
            deltaTime = 0.1f;
        }
        float pitchDiff = targetPitch - currentPitch;
        float baseMovement = pitchDiff * pitchSmoothFactor;
        if (Math.abs(pitchDiff) < 0.5f) {
            pitchTargetOffset = (rand.nextFloat() - 0.5f) * 0.8f;
            pitchNoiseVelocity = (rand.nextFloat() - 0.5f) * 0.3f;
        }
        float noiseAcceleration = (pitchTargetOffset - pitchNoiseOffset) * 2.0f - pitchNoiseVelocity * 0.8f;
        pitchNoiseVelocity += noiseAcceleration * deltaTime * 8.0f;
        pitchNoiseOffset += pitchNoiseVelocity * deltaTime * 6.0f;
        pitchNoiseOffset = Math.max(-1.2f, Math.min(1.2f, pitchNoiseOffset));
        pitchNoiseVelocity = Math.max(-2.0f, Math.min(2.0f, pitchNoiseVelocity));
        float movementSpeed = Math.abs(baseMovement);
        float noiseIntensity = 0.3f + movementSpeed * 0.5f;
        float totalMovement = baseMovement + pitchNoiseOffset * noiseIntensity;
        float easedMovement = easeOutCubic(totalMovement);
        easedMovement = Math.max(-3.0f, Math.min(3.0f, easedMovement));
        return easedMovement;
    }
    
    private float easeOutCubic(float x) {
        float t = Math.max(0, Math.min(1, Math.abs(x) / 2.0f));
        float eased = 1 - (float)Math.pow(1 - t, 3);
        return x < 0 ? -eased : eased;
    }
}
