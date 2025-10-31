package com.zerwhit.core.module;

import net.minecraft.client.Minecraft;

public class ModuleSprint extends Module {
    public ModuleSprint() {
        super("Sprint", true, "Movement");
        addConfig("OmniDirectional", false);
        addConfig("Mode", "Legit");
    }

    @Override
    public void onModuleTick() {
        if (!enabled || mc.thePlayer == null) return;
        
        boolean omniDirectional = (Boolean) getConfig("OmniDirectional");
        String mode = (String) getConfig("Mode");
        
        switch (mode) {
            case "Legit":
                if (omniDirectional) {
                    if (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0) {
                        mc.thePlayer.setSprinting(true);
                    }
                } else {
                    if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isCollidedHorizontally) {
                        mc.thePlayer.setSprinting(true);
                    }
                }
                break;
            case "Rage":
                mc.thePlayer.setSprinting(true);
                break;
        }
    }
    
    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Legit":
                    setConfig("Mode", "Rage");
                    break;
                case "Rage":
                    setConfig("Mode", "Legit");
                    break;
                default:
                    setConfig("Mode", "Legit");
            }
        }
    }
}