package com.zerwhit.core.module;

import net.minecraft.client.Minecraft;

public class ModuleFly extends Module {
    public ModuleFly() {
        super("Fly", false, "Movement");
        addConfig("Speed", 1.0);
        addConfig("AntiKick", true);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.capabilities.isFlying = true;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.capabilities.isFlying = false;
        }
    }

    @Override
    public void onModuleTick() {
        if (!enabled || mc.thePlayer == null) return;
        
        double speed = (Double) getConfig("Speed");
        boolean antiKick = (Boolean) getConfig("AntiKick");
        
        mc.thePlayer.capabilities.setFlySpeed((float) speed * 0.05f);
        mc.thePlayer.capabilities.isFlying = true;
        
        if (antiKick && mc.thePlayer.ticksExisted % 20 == 0) {
            mc.thePlayer.motionY = -0.04;
        }
    }
}