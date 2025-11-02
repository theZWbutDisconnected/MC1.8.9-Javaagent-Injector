package com.zerwhit.core.module.movement;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class ModuleSprint extends ModuleBase implements ITickableModule {
    public ModuleSprint() {
        super("Sprint", true, "Movement");
        addConfig("OmniDirectional", false);
        addConfig("Mode", "Legit");
    }

    @Override
    public void onModuleTick() {
        boolean omniDirectional = (Boolean) getConfig("OmniDirectional");
        String mode = (String) getConfig("Mode");
        
        switch (mode) {
            case "Legit":
                if (omniDirectional) {
                    if (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                        mc.thePlayer.setSprinting(true);
                    }
                } else {
                    if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isCollidedHorizontally) {
                        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                        mc.thePlayer.setSprinting(true);
                    }
                }
                break;
            case "Rage":
                mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING));
                mc.thePlayer.setSprinting(true);
                break;
        }
    }

    @Override
    public void onDisable() {
        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING));
        mc.thePlayer.setSprinting(false);
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