package org.zerwhit.core.module.movement;

import org.zerwhit.core.module.IEventModule;
import org.zerwhit.core.module.ModuleBase;
import net.minecraft.util.MathHelper;

public class ModuleMovementFix extends ModuleBase implements IEventModule {
    
    public ModuleMovementFix() {
        super("MovementFix", true, "Movement");
        addConfig("Mode", "Strafe");
        addConfig("Strict", false);
    }
    
    @Override
    public void onEvent(String eventName, Object... args) {
        if ("moveEntityWithHeading".equals(eventName) && args.length >= 2) {
            float strafe = (Float) args[0];
            float forward = (Float) args[1];
            
            if (mc.thePlayer != null) {
                handleMovementFix(strafe, forward);
            }
        }
    }
    
    private void handleMovementFix(float strafe, float forward) {
        String mode = (String) getConfig("Mode");
        boolean strict = (Boolean) getConfig("Strict");
        
        if (mode.equals("Strafe")) {
            // Strafe movement fix - ensures proper movement direction
            if (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0) {
                float yaw = mc.thePlayer.rotationYaw;
                
                // Calculate movement direction based on input
                float moveForward = mc.thePlayer.moveForward;
                float moveStrafe = mc.thePlayer.moveStrafing;
                
                // Normalize movement inputs
                float magnitude = (float) Math.sqrt(moveForward * moveForward + moveStrafe * moveStrafe);
                if (magnitude >= 0.01F) {
                    magnitude = 1.0F / magnitude;
                    moveForward *= magnitude;
                    moveStrafe *= magnitude;
                    
                    // Calculate movement angles
                    float sin = MathHelper.sin(yaw * (float) Math.PI / 180.0F);
                    float cos = MathHelper.cos(yaw * (float) Math.PI / 180.0F);
                    
                    // Apply corrected movement
                    mc.thePlayer.motionX += (moveStrafe * cos - moveForward * sin) * 0.02F;
                    mc.thePlayer.motionZ += (moveForward * cos + moveStrafe * sin) * 0.02F;
                    
                    if (strict) {
                        // Strict mode - prevent diagonal speed boost
                        float motionMagnitude = (float) Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
                        if (motionMagnitude > 0.25F) {
                            float factor = 0.25F / motionMagnitude;
                            mc.thePlayer.motionX *= factor;
                            mc.thePlayer.motionZ *= factor;
                        }
                    }
                }
            }
        } else if (mode.equals("Silent")) {
            // Silent movement fix - adjusts rotation for proper movement
            if (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0) {
                float yaw = getMovementDirection();
                if (yaw != mc.thePlayer.rotationYaw) {
                    // Temporarily adjust rotation for movement calculation
                    float originalYaw = mc.thePlayer.rotationYaw;
                    mc.thePlayer.rotationYaw = yaw;
                    
                    // Movement will be calculated with corrected rotation
                    // Rotation will be restored in post-update
                    mc.thePlayer.rotationYaw = originalYaw;
                }
            }
        }
    }
    
    private float getMovementDirection() {
        float moveForward = mc.thePlayer.moveForward;
        float moveStrafe = mc.thePlayer.moveStrafing;
        float yaw = mc.thePlayer.rotationYaw;
        
        // Calculate the intended movement direction
        if (moveForward != 0 && moveStrafe != 0) {
            // Diagonal movement
            if (moveForward > 0) {
                yaw += moveStrafe > 0 ? -45.0F : 45.0F;
            } else {
                yaw += moveStrafe > 0 ? -135.0F : 135.0F;
            }
        } else if (moveStrafe != 0) {
            // Strafe only
            if (moveStrafe > 0) {
                yaw -= 90.0F;
            } else {
                yaw += 90.0F;
            }
        }
        
        return yaw;
    }
    
    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Strafe":
                    setConfig("Mode", "Silent");
                    break;
                case "Silent":
                    setConfig("Mode", "Strafe");
                    break;
                default:
                    setConfig("Mode", "Strafe");
            }
        }
    }
    
    @Override
    public String getDisplayName() {
        String mode = (String) getConfig("Mode");
        return name + " [" + mode + "]";
    }
}