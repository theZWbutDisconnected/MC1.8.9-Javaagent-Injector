package com.zerwhit.core.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

/**
 * Rotation Manager - 玩家旋转管理类
 * 提供平滑的旋转过渡和多种旋转模式
 */
public class RotationManager {
    private static final RotationManager INSTANCE = new RotationManager();
    
    private Minecraft mc;
    
    // 旋转状态
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    private boolean isRotating = false;
    private long rotationStartTime = 0;
    
    // 旋转配置
    private RotationMode rotationMode = RotationMode.LINEAR;
    private float rotationSpeed = 180.0f; // 度/秒
    private float maxRotationSpeed = 360.0f;
    private float rotationThreshold = 0.1f; // 旋转完成阈值
    
    // 旋转模式枚举
    public enum RotationMode {
        LINEAR,      // 线性插值
        SMOOTH,      // 平滑插值
        INSTANT,     // 瞬间旋转
        CUSTOM       // 自定义插值
    }
    
    private RotationManager() {
        this.mc = Minecraft.getMinecraft();
    }
    
    public static RotationManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 设置目标旋转角度
     * @param yaw 目标偏航角
     * @param pitch 目标俯仰角
     */
    public void setTargetRotation(float yaw, float pitch) {
        // 规范化角度
        this.targetYaw = normalizeAngle(yaw);
        this.targetPitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);
        
        // 获取当前玩家角度
        if (mc.thePlayer != null) {
            this.currentYaw = normalizeAngle(mc.thePlayer.rotationYaw);
            this.currentPitch = mc.thePlayer.rotationPitch;
        }
        
        this.isRotating = true;
        this.rotationStartTime = System.currentTimeMillis();
    }
    
    /**
     * 设置目标旋转到实体
     * @param targetX 目标X坐标
     * @param targetY 目标Y坐标
     * @param targetZ 目标Z坐标
     */
    public void setTargetRotationToEntity(double targetX, double targetY, double targetZ) {
        if (mc.thePlayer == null) return;
        
        double deltaX = targetX - mc.thePlayer.posX;
        double deltaY = targetY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = targetZ - mc.thePlayer.posZ;
        
        // 计算距离
        double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
        
        // 计算偏航角
        float yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        
        // 计算俯仰角
        float pitch = (float) (-(Math.atan2(deltaY, distance) * 180.0 / Math.PI));
        
        setTargetRotation(yaw, pitch);
    }
    
    /**
     * 更新旋转（每tick调用）
     */
    public void updateRotation() {
        if (!isRotating || mc.thePlayer == null) return;
        
        // 获取当前时间
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - rotationStartTime) / 1000.0f;
        
        // 计算角度差值
        float yawDifference = getAngleDifference(currentYaw, targetYaw);
        float pitchDifference = targetPitch - currentPitch;
        
        // 检查是否完成旋转
        if (Math.abs(yawDifference) < rotationThreshold && Math.abs(pitchDifference) < rotationThreshold) {
            isRotating = false;
            currentYaw = targetYaw;
            currentPitch = targetPitch;
            applyRotation();
            return;
        }
        
        // 根据旋转模式计算新角度
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
        
        // 应用旋转到玩家
        applyRotation();
        
        rotationStartTime = currentTime;
    }
    
    /**
     * 线性插值旋转
     */
    private void updateLinearRotation(float deltaTime, float yawDifference, float pitchDifference) {
        float maxRotationThisFrame = rotationSpeed * deltaTime;
        
        // 偏航角插值
        if (Math.abs(yawDifference) > maxRotationThisFrame) {
            currentYaw += Math.signum(yawDifference) * maxRotationThisFrame;
        } else {
            currentYaw = targetYaw;
        }
        
        // 俯仰角插值
        if (Math.abs(pitchDifference) > maxRotationThisFrame) {
            currentPitch += Math.signum(pitchDifference) * maxRotationThisFrame;
        } else {
            currentPitch = targetPitch;
        }
        
        // 规范化角度
        currentYaw = normalizeAngle(currentYaw);
        currentPitch = MathHelper.clamp_float(currentPitch, -90.0f, 90.0f);
    }
    
    /**
     * 平滑插值旋转
     */
    private void updateSmoothRotation(float deltaTime, float yawDifference, float pitchDifference) {
        // 使用缓动函数实现平滑过渡
        float smoothFactor = 0.2f; // 平滑因子
        
        // 偏航角平滑插值
        float yawStep = yawDifference * smoothFactor;
        if (Math.abs(yawStep) > rotationSpeed * deltaTime) {
            yawStep = Math.signum(yawStep) * rotationSpeed * deltaTime;
        }
        currentYaw += yawStep;
        
        // 俯仰角平滑插值
        float pitchStep = pitchDifference * smoothFactor;
        if (Math.abs(pitchStep) > rotationSpeed * deltaTime) {
            pitchStep = Math.signum(pitchStep) * rotationSpeed * deltaTime;
        }
        currentPitch += pitchStep;
        
        // 规范化角度
        currentYaw = normalizeAngle(currentYaw);
        currentPitch = MathHelper.clamp_float(currentPitch, -90.0f, 90.0f);
    }
    
    /**
     * 瞬间旋转
     */
    private void updateInstantRotation() {
        currentYaw = targetYaw;
        currentPitch = targetPitch;
        isRotating = false;
    }
    
    /**
     * 自定义插值旋转
     */
    private void updateCustomRotation(float deltaTime, float yawDifference, float pitchDifference) {
        // 自定义插值逻辑，可以根据需要扩展
        updateLinearRotation(deltaTime, yawDifference, pitchDifference);
    }
    
    /**
     * 应用旋转到玩家
     */
    private void applyRotation() {
        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYaw = currentYaw;
            mc.thePlayer.rotationPitch = currentPitch;
            
            // 更新头部旋转
            mc.thePlayer.prevRotationYaw = currentYaw;
            mc.thePlayer.prevRotationPitch = currentPitch;
        }
    }
    
    /**
     * 获取两个角度之间的最小差值
     */
    private float getAngleDifference(float angle1, float angle2) {
        float difference = angle2 - angle1;
        while (difference < -180.0f) difference += 360.0f;
        while (difference > 180.0f) difference -= 360.0f;
        return difference;
    }
    
    /**
     * 规范化角度到 [-180, 180] 范围
     */
    private float normalizeAngle(float angle) {
        angle %= 360.0f;
        if (angle > 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
    
    /**
     * 停止旋转
     */
    public void stopRotation() {
        isRotating = false;
    }
    
    /**
     * 检查是否正在旋转
     */
    public boolean isRotating() {
        return isRotating;
    }
    
    /**
     * 获取当前旋转进度 (0.0 - 1.0)
     */
    public float getRotationProgress() {
        if (!isRotating) return 1.0f;
        
        float yawDiff = Math.abs(getAngleDifference(currentYaw, targetYaw));
        float pitchDiff = Math.abs(targetPitch - currentPitch);
        
        float maxDiff = Math.max(yawDiff, pitchDiff);
        return 1.0f - (maxDiff / 180.0f);
    }
    
    // 配置设置方法
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
    
    // 获取当前配置
    public RotationMode getRotationMode() { return rotationMode; }
    public float getRotationSpeed() { return rotationSpeed; }
    public float getMaxRotationSpeed() { return maxRotationSpeed; }
    public float getRotationThreshold() { return rotationThreshold; }
    
    // 获取当前旋转状态
    public float getCurrentYaw() { return currentYaw; }
    public float getCurrentPitch() { return currentPitch; }
    public float getTargetYaw() { return targetYaw; }
    public float getTargetPitch() { return targetPitch; }
}