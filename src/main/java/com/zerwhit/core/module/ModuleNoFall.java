package com.zerwhit.core.module;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;

public class ModuleNoFall extends Module {
    public ModuleNoFall() {
        super("NoFall", true, "Movement");
        addConfig("Mode", "Packet");
    }

    @Override
    public void onModuleTick() {
        if (!enabled || mc.thePlayer == null) return;
        
        String mode = (String) getConfig("Mode");
        
        if (mc.thePlayer.fallDistance > 2.5f) {
            switch (mode) {
                case "Packet":
                    mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(true));
                    break;
                case "Spoof":
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        mc.thePlayer.onGround = true;
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
                case "Packet":
                    setConfig("Mode", "Spoof");
                    break;
                case "Spoof":
                    setConfig("Mode", "Packet");
                    break;
                default:
                    setConfig("Mode", "Packet");
            }
        }
    }
}