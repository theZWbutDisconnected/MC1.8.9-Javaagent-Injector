package com.zerwhit.core.module.movement;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;

public class ModuleSpeed extends ModuleBase implements ITickableModule {
    public ModuleSpeed() {
        super("Speed", false, "Movement");
        addConfig("Speed", 1.5);
        addConfig("Mode", "BHop");
    }

    @Override
    public void onModuleTick() {
        double speed = (Double) getConfig("Speed");
        String mode = (String) getConfig("Mode");
        
        if (mc.thePlayer.onGround) {
            switch (mode) {
                case "Vanilla":
                    mc.thePlayer.motionX *= speed;
                    mc.thePlayer.motionZ *= speed;
                    break;
                case "BHop":
                    if (mc.thePlayer.moveForward > 0) {
                        mc.thePlayer.motionX = Math.sin(Math.toRadians(-mc.thePlayer.rotationYaw)) * speed;
                        mc.thePlayer.motionZ = Math.cos(Math.toRadians(mc.thePlayer.rotationYaw)) * speed;
                    }
                    break;
            }
        }
    }
    
    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Vanilla":
                    setConfig("Mode", "BHop");
                    break;
                case "BHop":
                    setConfig("Mode", "Vanilla");
                    break;
                default:
                    setConfig("Mode", "Vanilla");
            }
        }
    }
    
    @Override
    public double getMaxDoubleValueForConfig(String key) {
        if ("Speed".equals(key)) {
            return 5.0;
        }
        return super.getMaxDoubleValueForConfig(key);
    }
}