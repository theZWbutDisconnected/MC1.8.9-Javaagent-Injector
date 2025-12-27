package org.zerwhit.core.module.player;

import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.KeyRobot;
import net.minecraft.util.*;


public class ModuleScaffold extends ModuleBase implements ITickableModule {
    private BlockPos targetPos = null;
    private boolean playerAtEdge = false;
    private boolean playerWentToNextBlock = false;
    public ModuleScaffold() {
        super("Scaffold", false, "Movement");
        addConfig("Sprint", false);
        addConfig("Mode", "Legit");
        addConfig("RequireSneak", false);
        addConfig("Slient", false);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onModuleTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        String mode = (String) getConfig("Mode");
        boolean requireSneak = (boolean) getConfig("RequireSneak");
        EntityPlayer player = mc.thePlayer;
        double mX = player.motionX < 0.0 ? -1.0 : (player.motionX > 0.0 ? 1.0 : 0.0);
        double mZ = player.motionZ < 0.0 ? -1.0 : (player.motionZ > 0.0 ? 1.0 : 0.0);
        BlockPos currentPos = player.playerLocation;
        BlockPos posWillBe = currentPos.add(mX, 0, mZ);
        boolean flag = false;
        flag = mc.theWorld.getBlockState(posWillBe).getBlock().isAir(mc.theWorld, posWillBe);
        if (flag)
            targetPos = posWillBe;
        if (mode.equals("Legit")) {
            // Add null checks for currentPos and player
            if (currentPos != null && player != null) {
                playerAtEdge = Math.abs(player.posX - currentPos.getX()) >= 0.4 || Math.abs(player.posZ - currentPos.getZ()) >= 0.4;
                playerWentToNextBlock = currentPos.equals(posWillBe);
                
                if (targetPos != null && (playerAtEdge && !player.isSneaking())) {
                    KeyRobot.pressKey(Keyboard.KEY_LSHIFT);
                    playerAtEdge = false;
                }
                if (targetPos != null && (Math.abs(player.posX - targetPos.getX()) <= 0.5 || Math.abs(player.posZ - targetPos.getZ()) <= 0.5)) {
                    // Add null check for playerController and held item
                    if (mc.playerController != null && player.getHeldItem() != null) {
                        mc.playerController.sendUseItem(player, mc.theWorld, player.getHeldItem());
                    }
                }
                if (targetPos != null && (playerWentToNextBlock && player.isSneaking())) {
                    KeyRobot.releaseKey(Keyboard.KEY_LSHIFT);
                    targetPos = null;
                    playerAtEdge = false;
                    playerWentToNextBlock = false;
                }
            }
        }
    }

    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Legit":
                    setConfig("Mode", "Clutch");
                    break;
                case "Clutch":
                    setConfig("Mode", "Telly");
                    break;
                case "Telly":
                    setConfig("Mode", "Legit");
                    break;
                default:
                    setConfig("Mode", "Legit");
            }
        }
    }
    
    @Override
    public double getMaxValueForConfig(String key) {
        return super.getMaxValueForConfig(key);
    }
}
