package com.zerwhit.core.module.movement;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;

public class ModuleEdgeSneak extends ModuleBase implements ITickableModule {
    private long lastSneakTime = 0;
    private boolean wasOnEdge = false;
    private boolean isSneaking = false;

    public ModuleEdgeSneak() {
        super("EdgeSneak", false, "Movement");
        addConfig("Distance", 1.0);
        addConfig("Mode", "Auto");
        addConfig("DetectionRadius", 0.5);
    }

    @Override
    public void onModuleTick() {
        double distance = (Double) getConfig("Distance");
        String mode = (String) getConfig("Mode");
        double radius = (Double) getConfig("DetectionRadius");
        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
        mc.thePlayer.movementInput.sneak = true;
        mc.thePlayer.sendQueue.addToSendQueue(new C0CPacketInput(mc.thePlayer.moveStrafing, mc.thePlayer.moveForward, mc.thePlayer.movementInput.jump, mc.thePlayer.movementInput.sneak)); 
        mc.thePlayer.setSneaking(true);
    }

    @Override
    public void onDisable() {
        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
        mc.thePlayer.movementInput.sneak = false;
        mc.thePlayer.sendQueue.addToSendQueue(new C0CPacketInput(mc.thePlayer.moveStrafing, mc.thePlayer.moveForward, mc.thePlayer.movementInput.jump, mc.thePlayer.movementInput.sneak)); 
        mc.thePlayer.setSneaking(false);
    }

    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Auto":
                    setConfig("Mode", "Toggle");
                    break;
                case "Toggle":
                    setConfig("Mode", "Auto");
                    break;
                default:
                    setConfig("Mode", "Auto");
            }
        }
    }
}