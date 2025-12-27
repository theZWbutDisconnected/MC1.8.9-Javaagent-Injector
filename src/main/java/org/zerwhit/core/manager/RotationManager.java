package org.zerwhit.core.manager;

import org.zerwhit.core.data.Meta;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public final class RotationManager {
    public Entity rendererViewEntity;

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
}