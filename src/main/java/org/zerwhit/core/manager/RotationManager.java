package org.zerwhit.core.manager;

import net.minecraft.client.entity.EntityPlayerSP;
import org.zerwhit.core.data.Meta;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import net.minecraft.util.MathHelper;
import java.util.Random;

public final class RotationManager {
    public Entity rendererViewEntity;
    private Random rand = new Random();
    private long lastPitchUpdateTime = 0L;
    private float pitchNoiseOffset = 0f;
    private float pitchNoiseVelocity = 0f;
    private float pitchTargetOffset = 0f;
    private float pitchSmoothFactor = 0.15f;

    public static float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0f, Math.min(180.0f, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }
        return angle;
    }

    public float normalizeAngle(float angle) {
        angle %= 360.0f;
        if (angle > 180.0f) {
            angle -= 360.0f;
        } else if (angle < -180.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    public float normalizeAngleTo360(float angle) {
        angle %= 360.0f;
        if (angle < 0.0f) {
            angle += 360.0f;
        }
        return angle;
    }

    public boolean isViewExceedingBoundingBox(float currentYaw, Entity target, Entity player) {
        if (target == null || player == null) return false;
        
        double playerX = player.posX;
        double playerY = player.posY + player.getEyeHeight();
        double playerZ = player.posZ;
        
        double targetX = target.posX;
        double targetY = target.posY + target.getEyeHeight();
        double targetZ = target.posZ;
        
        double deltaX = targetX - playerX;
        double deltaZ = targetZ - playerZ;
        
        double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 0.1) return false;
        
        double targetYaw = Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI - 90.0;
        double yawDiff = normalizeAngle((float)(targetYaw - currentYaw));
        
        double boundingBoxWidth = target.width / 2.0;
        double boundingBoxDepth = target.width / 2.0;
        
        double angleThreshold = Math.toDegrees(Math.atan2(boundingBoxWidth, distance)) * 0.7;
        
        return Math.abs(yawDiff) < angleThreshold;
    }
    
    public float calculateSmoothPitchChange(float currentPitch, float targetPitch) {
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

    public static float wrapAngleDiff(float angle1, float angle2) {
        float diff = angle1 - angle2;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return diff;
    }

    public static float quantizeAngle(float angle) {
        return (float) ((double) angle - (double) angle % (double) 0.0096f);
    }

    public static float[] getRotationsTo(double x, double y, double z, float currentYaw, float currentPitch) {
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
        
        return new float[]{yaw, pitch};
    }

    public static MovingObjectPosition rayTrace(float yaw, float pitch, double distance, float partialTicks, EntityPlayerSP player) {
        if (player == null) return null;
        Vec3 vec3 = player.getPositionEyes(partialTicks);
        Vec3 vec31 = getVectorForRotation(pitch, yaw);
        Vec3 vec32 = vec3.addVector(vec31.xCoord * distance, vec31.yCoord * distance, vec31.zCoord * distance);
        return player.worldObj.rayTraceBlocks(vec3, vec32, false, false, true);
    }

    public static MovingObjectPosition rayTrace(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        Vec3 targetPos = clampVecToBox(eyePos, entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize));
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    public static Vec3 clampVecToBox(Vec3 vector, AxisAlignedBB boundingBox) {
        double[] coords = new double[]{vector.xCoord, vector.yCoord, vector.zCoord};
        double[] minCoords = new double[]{boundingBox.minX, boundingBox.minY, boundingBox.minZ};
        double[] maxCoords = new double[]{boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ};
        for (int i = 0; i < 3; ++i) {
            if (coords[i] > maxCoords[i]) {
                coords[i] = maxCoords[i];
                continue;
            }
            if (!(coords[i] < minCoords[i])) continue;
            coords[i] = minCoords[i];
        }
        return new Vec3(coords[0], coords[1], coords[2]);
    }

    private static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double)(f1 * f2), (double)f3, (double)(f * f2));
    }
}