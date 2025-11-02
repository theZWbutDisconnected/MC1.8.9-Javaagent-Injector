package com.zerwhit.core.module.combat;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;

public class ModuleAutoBlock extends ModuleBase {
    private boolean isBlocking = false;

    public ModuleAutoBlock() {
        super("AutoBlock", true, "Combat");
        addConfig("Mode", "Smart");
        addConfig("HoldTime", 200);
    }

    public void onPlayerHurt() {
        String mode = (String) getConfig("Mode");
        int holdTime = (Integer) getConfig("HoldTime");

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) return;

        if (!(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) return;

        switch (mode) {
            case "Smart":
                if (!isBlocking) {
                    startBlocking();

                    new Thread(() -> {
                        try {
                            Thread.sleep(holdTime);
                            if (isBlocking) {
                                stopBlocking();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                break;

            case "Toggle":
                if (!isBlocking) {
                    startBlocking();
                }
                break;
        }
    }

    private void startBlocking() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
        WorldServer worldServer = (WorldServer) mc.thePlayer.worldObj;
        worldServer.getEntityTracker().func_151248_b(mc.thePlayer, new S0BPacketAnimation(mc.thePlayer, EnumAction.BLOCK.ordinal()));
        isBlocking = true;
    }

    public void stopBlocking() {
        Minecraft mc = Minecraft.getMinecraft();
        if (isBlocking) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                    BlockPos.ORIGIN,
                    EnumFacing.DOWN
            ));
            isBlocking = false;
        }
    }

    @Override
    public void onDisable() {
        stopBlocking();
    }

    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Smart":
                    setConfig("Mode", "Toggle");
                    break;
                case "Toggle":
                    setConfig("Mode", "Smart");
                    break;
                default:
                    setConfig("Mode", "Smart");
            }
        }
    }
    
    @Override
    public int getMaxValueForConfig(String key) {
        if ("HoldTime".equals(key)) {
            return 2000;
        }
        return super.getMaxValueForConfig(key);
    }
}