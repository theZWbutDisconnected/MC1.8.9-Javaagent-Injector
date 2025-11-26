package com.zerwhit.core.module.movement;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

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
        BlockPos
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
