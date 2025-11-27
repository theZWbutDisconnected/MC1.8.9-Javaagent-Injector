package com.zerwhit.core.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class RotationManager {
    private static final RotationManager INSTANCE = new RotationManager();
    
    private Minecraft mc;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    private boolean isRotating = false;
    private long rotationStartTime = 0;

    private RotationMode rotationMode = RotationMode.LINEAR;
    private float rotationSpeed = 180.0f;
    private float maxRotationSpeed = 360.0f;
    private float rotationThreshold = 0.1f;

    public Entity rendererViewEntity;

    public enum RotationMode {
        LINEAR,
        SMOOTH,
        INSTANT,
        CUSTOM
    }
    
    private RotationManager() {
        this.mc = Minecraft.getMinecraft();
    }
    
    public static RotationManager getInstance() {
        return INSTANCE;
    }

    public void setTargetRotation(float yaw, float pitch) {
        this.targetYaw = normalizeAngle(yaw);
        this.targetPitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
        if (mc.thePlayer != null) {
            this.currentYaw = normalizeAngle(mc.thePlayer.rotationYaw);
            this.currentPitch = mc.thePlayer.rotationPitch;
        }
        
        this.isRotating = true;
        this.rotationStartTime = System.currentTimeMillis();
    }

    public void setTargetRotationToPos(double targetX, double targetY, double targetZ) {
        if (mc.thePlayer == null) return;
        
        double deltaX = targetX - mc.thePlayer.posX;
        double deltaY = targetY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = targetZ - mc.thePlayer.posZ;
        double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(deltaY, distance) * 180.0 / Math.PI));
        
        setTargetRotation(yaw, pitch);
    }

    public void updateRotation() {
        if (!isRotating || mc.thePlayer == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - rotationStartTime) / 1000.0f;

        float yawDifference = getAngleDifference(currentYaw, targetYaw);
        float pitchDifference = targetPitch - currentPitch;

        if (Math.abs(yawDifference) < rotationThreshold && Math.abs(pitchDifference) < rotationThreshold) {
            isRotating = false;
            currentYaw = targetYaw;
            currentPitch = targetPitch;
            applyRotation();
            return;
        }

        switch (rotationMode) {
            case LINEAR:
                updateLinearRotation(deltaTime, yawDifference, pitchDifference);
                break;
            case SMOOTH:
                updateSmoothRotation(deltaTime, yawDifference, pitchDifference);
                break;
            case INSTANT:
                updateInstantRotation();
                break;
            case CUSTOM:
                updateCustomRotation(deltaTime, yawDifference, pitchDifference);
                break;
        }

        applyRotation();
        rotationStartTime = currentTime;
    }

    private void updateLinearRotation(float deltaTime, float yawDifference, float pitchDifference) {
        float maxRotationThisFrame = rotationSpeed * deltaTime;

        if (Math.abs(yawDifference) > maxRotationThisFrame) {
            currentYaw += Math.signum(yawDifference) * maxRotationThisFrame;
        } else {
            currentYaw = targetYaw;
        }

        if (Math.abs(pitchDifference) > maxRotationThisFrame) {
            currentPitch += Math.signum(pitchDifference) * maxRotationThisFrame;
        } else {
            currentPitch = targetPitch;
        }

        currentYaw = normalizeAngle(currentYaw);
        currentPitch = MathHelper.clamp_float(currentPitch, -90.0f, 90.0f);
    }

    private void updateSmoothRotation(float deltaTime, float yawDifference, float pitchDifference) {
        float smoothFactor = 0.2f;
        float yawStep = yawDifference * smoothFactor;
        if (Math.abs(yawStep) > rotationSpeed * deltaTime) {
            yawStep = Math.signum(yawStep) * rotationSpeed * deltaTime;
        }
        currentYaw += yawStep;

        float pitchStep = pitchDifference * smoothFactor;
        if (Math.abs(pitchStep) > rotationSpeed * deltaTime) {
            pitchStep = Math.signum(pitchStep) * rotationSpeed * deltaTime;
        }
        currentPitch += pitchStep;

        currentYaw = normalizeAngle(currentYaw);
        currentPitch = MathHelper.clamp_float(currentPitch, -90.0f, 90.0f);
    }

    private void updateInstantRotation() {
        currentYaw = targetYaw;
        currentPitch = targetPitch;
        isRotating = false;
    }

    private void updateCustomRotation(float deltaTime, float yawDifference, float pitchDifference) {
        updateLinearRotation(deltaTime, yawDifference, pitchDifference);
    }

    private void applyRotation() {
        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = currentYaw;
            mc.thePlayer.rotationPitch = currentPitch;
        }
    }

    private float getAngleDifference(float angle1, float angle2) {
        float difference = angle2 - angle1;
        while (difference < -180.0f) difference += 360.0f;
        while (difference > 180.0f) difference -= 360.0f;
        return difference;
    }

    private float normalizeAngle(float angle) {
        angle %= 360.0f;
        if (angle > 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }

    public void stopRotation() {
        isRotating = false;
    }

    public boolean isRotating() {
        return isRotating;
    }

    public float getRotationProgress() {
        if (!isRotating) return 1.0f;
        
        float yawDiff = Math.abs(getAngleDifference(currentYaw, targetYaw));
        float pitchDiff = Math.abs(targetPitch - currentPitch);
        
        float maxDiff = Math.max(yawDiff, pitchDiff);
        return 1.0f - (maxDiff / 180.0f);
    }

    public void setRotationMode(RotationMode mode) {
        this.rotationMode = mode;
    }
    
    public void setRotationSpeed(float speed) {
        this.rotationSpeed = MathHelper.clamp_float(speed, 0.1f, maxRotationSpeed);
    }
    
    public void setMaxRotationSpeed(float maxSpeed) {
        this.maxRotationSpeed = Math.max(1.0f, maxSpeed);
        this.rotationSpeed = Math.min(rotationSpeed, maxRotationSpeed);
    }
    
    public void setRotationThreshold(float threshold) {
        this.rotationThreshold = Math.max(0.01f, threshold);
    }

    public RotationMode getRotationMode() { return rotationMode; }
    public float getRotationSpeed() { return rotationSpeed; }
    public float getMaxRotationSpeed() { return maxRotationSpeed; }
    public float getRotationThreshold() { return rotationThreshold; }

    public float getCurrentYaw() { return currentYaw; }
    public float getCurrentPitch() { return currentPitch; }
    public float getTargetYaw() { return targetYaw; }
    public float getTargetPitch() { return targetPitch; }
}