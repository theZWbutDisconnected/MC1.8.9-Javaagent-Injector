package com.zerwhit.core.module.movement;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import com.zerwhit.core.util.KeyRobot;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class ModuleEdgeSneak extends ModuleBase implements ITickableModule {
    public boolean hasSneaked = false;
    public ModuleEdgeSneak() {
        super("EdgeSneak", false, "Movement");
        addConfig("Distance", 1.0);
        addConfig("DetectionRadius", 0.5);
    }

    @Override
    public void onModuleTick() {
        double distance = (Double) getConfig("Distance");
        double radius = (Double) getConfig("DetectionRadius");
        Vec3 dir = new Vec3(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ).normalize();
        List<AxisAlignedBB> blockPosList = mc.theWorld.getCollisionBoxes(mc.thePlayer.getEntityBoundingBox().expand(radius, 0, radius).offset(dir.xCoord * distance, -1, dir.zCoord * distance));
        if (blockPosList.isEmpty() && !hasSneaked) {
            triggerShift();
        } else {
            disableShift();
        }
    }

    @Override
    public void onDisable() {
        disableShift();
    }

    private void triggerShift() {
        hasSneaked = true;
        KeyRobot.pressKey(KeyEvent.VK_SHIFT);
        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING));
        mc.thePlayer.movementInput.sneak = true;
        mc.thePlayer.sendQueue.addToSendQueue(new C0CPacketInput(mc.thePlayer.moveStrafing, mc.thePlayer.moveForward, mc.thePlayer.movementInput.jump, true));
        mc.thePlayer.setSneaking(true);
    }

    private void disableShift() {
        hasSneaked = false;
        KeyRobot.releaseKey(KeyEvent.VK_SHIFT);
        mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING));
        mc.thePlayer.movementInput.sneak = false;
        mc.thePlayer.sendQueue.addToSendQueue(new C0CPacketInput(mc.thePlayer.moveStrafing, mc.thePlayer.moveForward, mc.thePlayer.movementInput.jump, false));
        mc.thePlayer.setSneaking(false);
    }

    @Override
    public double getMaxValueForConfig(String key) {
        if ("Distance".equals(key)) {
            return 5.0;
        } else if ("DetectionRadius".equals(key)) {
            return 3.0;
        }
        return super.getMaxValueForConfig(key);
    }
}