package com.zerwhit.core.module.movement;

import com.zerwhit.core.manager.RotationManager;
import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.util.*;

public class ModuleScaffold extends ModuleBase implements ITickableModule {
    private BlockPos targetPos;
    
    public ModuleScaffold() {
        super("Scaffold", false, "Movement");
        addConfig("Range", 3.0);
        addConfig("Sprint", false);
        addConfig("Mode", "Legit");
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onModuleTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        setRotationMode(RotationManager.RotationMode.SMOOTH);
        setRotationSpeed(560.0F);
        setRotationThreshold(0.5F);
        Vec3 lookVec = RotationManager.getInstance().rendererViewEntity.getLookVec();
        Vec3 dir = new Vec3(-lookVec.xCoord, -lookVec.yCoord, -lookVec.zCoord).normalize();
        double range = (Double) getConfig("Range");
        double playerX = mc.thePlayer.posX;
        double playerY = mc.thePlayer.posY;
        double playerZ = mc.thePlayer.posZ;
        BlockPos targetPos = null;
        for (double offset = 0; offset <= range; offset += 0.5) {
            double behindX = playerX + dir.xCoord * offset;
            double behindZ = playerZ + dir.zCoord * offset;
            for (int yOffset = 0; yOffset <= 3; yOffset++) {
                BlockPos checkPos = new BlockPos(behindX, playerY - yOffset, behindZ);
                BlockPos belowPos = new BlockPos(behindX, playerY - yOffset - 1, behindZ);
                Block currentBlock = mc.theWorld.getBlockState(checkPos).getBlock();
                Block belowBlock = mc.theWorld.getBlockState(belowPos).getBlock();
                if ((currentBlock instanceof BlockAir || currentBlock instanceof BlockLiquid) && 
                    !(belowBlock instanceof BlockAir) && !(belowBlock instanceof BlockLiquid)) {
                    targetPos = checkPos;
                    break;
                }
            }
            
            if (targetPos != null) break;
        }

        if (targetPos == null) {
            targetPos = new BlockPos(playerX, playerY - 1, playerZ);
        }

        if (targetPos != null) {
            rotMng.setTargetRotationToPos(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        }
    }

    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Legit":
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
        if ("Range".equals(key)) {
            return 6.0;
        } else if ("Delay".equals(key)) {
            return 10.0;
        }
        return super.getMaxValueForConfig(key);
    }
}
