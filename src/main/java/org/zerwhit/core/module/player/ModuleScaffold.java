package org.zerwhit.core.module.player;

import javafx.scene.input.KeyCode;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.WorldSettings;
import org.lwjgl.input.Keyboard;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.manager.RotationManager;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class ModuleScaffold extends ModuleBase implements ITickableModule {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private RotationManager rotMng = new RotationManager();
    private static final double[] placeOffsets = new double[]{
            0.03125,
            0.09375,
            0.15625,
            0.21875,
            0.28125,
            0.34375,
            0.40625,
            0.46875,
            0.53125,
            0.59375,
            0.65625,
            0.71875,
            0.78125,
            0.84375,
            0.90625,
            0.96875
    };

    private int lastSlot = -1;
    private int blockCount = -1;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;

    public ModuleScaffold() {
        super("Scaffold", false, "Movement");
        addConfig("Mode", "Telly");
        addConfig("Rotation", "Vanilla");
        setBindingKey(KeyCode.C);
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (mc.thePlayer != null) {
            this.lastSlot = mc.thePlayer.inventory.currentItem;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Meta.slientAimEnabled = false;

        if (mc.thePlayer != null && this.lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.lastSlot;
        }
    }

    @Override
    public void onModuleTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        String mode = (String) getConfig("Mode");

        if (mode == "Telly") handleTelly();
    }

    private void handleTelly() {
        String rotation = (String) getConfig("Rotation");
        BlockData target = getBlockData();
        if (target != null) {
            Meta.slientAimEnabled = true;
        }
    }

    private boolean canPlace() {
        return mc.thePlayer != null && mc.theWorld != null && !mc.thePlayer.isDead;
    }

    private void updateBlockCount() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        int count = BlockUtil.isBlock(stack) ? stack.stackSize : 0;
        this.blockCount = Math.min(this.blockCount, count);

        if (this.blockCount <= 0) {
            for (int i = 0; i < 9; i++) {
                ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(i);
                if (BlockUtil.isBlock(candidate)) {
                    mc.thePlayer.inventory.currentItem = i;
                    this.blockCount = candidate.stackSize;
                    break;
                }
            }
        }
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (this.stage != 0 && !this.shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ)
        );

        if (!BlockUtil.isReplaceable(targetPos)) {
            return null;
        }

        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 0; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    if (!BlockUtil.isReplaceable(pos) && !BlockUtil.isContainer(pos) &&
                            mc.thePlayer.getDistance((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <=
                                    (double) mc.playerController.getBlockReachDistance() &&
                            (this.stage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {

                        for (EnumFacing facing : EnumFacing.VALUES) {
                            if (facing != EnumFacing.DOWN) {
                                BlockPos blockPos = pos.offset(facing);
                                if (BlockUtil.isReplaceable(blockPos)) {
                                    positions.add(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (positions.isEmpty()) {
            return null;
        }

        positions.sort(Comparator.comparingDouble(
                o -> o.distanceSqToCenter((double) targetPos.getX() + 0.5, (double) targetPos.getY() + 0.5, (double) targetPos.getZ() + 0.5)
        ));

        BlockPos blockPos = positions.get(0);
        EnumFacing facing = getBestFacing(blockPos, targetPos);
        return facing == null ? null : new BlockData(blockPos, facing);
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing enumFacing = null;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                BlockPos pos = blockPos1.offset(facing);
                if (pos.getY() <= blockPos3.getY()) {
                    double distance = pos.distanceSqToCenter((double) blockPos3.getX() + 0.5, (double) blockPos3.getY() + 0.5, (double) blockPos3.getZ() + 0.5);
                    if (enumFacing == null || distance < offset || (distance == offset && facing == EnumFacing.UP)) {
                        offset = distance;
                        enumFacing = facing;
                    }
                }
            }
        }
        return enumFacing;
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3, boolean swing) {
        if (BlockUtil.isHoldingBlock() && this.blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), blockPos, enumFacing, vec3)) {
                if (mc.playerController.getCurrentGameType() != WorldSettings.GameType.CREATIVE) {
                    this.blockCount--;
                }
                if (swing) {
                    mc.thePlayer.swingItem();
                }
            }
        }
    }

    private float getCurrentYaw() {
        return mc.thePlayer.rotationYaw;
    }

    private float getCurrentPitch() {
        return mc.thePlayer.rotationPitch;
    }

    private boolean isForwardPressed() {
        return mc.gameSettings.keyBindForward.isKeyDown();
    }

    private boolean isAirBelow() {
        BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
        return BlockUtil.isReplaceable(pos);
    }

    private boolean isAirAbove() {
        BlockPos pos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ);
        return BlockUtil.isReplaceable(pos);
    }

    private float wrapAngleDiff(float angle1, float angle2) {
        float diff = angle1 - angle2;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return diff;
    }

    private float quantizeAngle(float angle) {
        return Math.round(angle / 45.0F) * 45.0F;
    }

    private float clampAngle(float angle, float max) {
        if (angle > max) return max;
        if (angle < -max) return -max;
        return angle;
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
        } else if (key.equals("Rotation")) {
            String currentRotation = (String) getConfig("Rotation");
            switch (currentRotation) {
                case "None":
                    setConfig("Rotation", "Vanilla");
                    break;
                case "Vanilla":
                    setConfig("Rotation", "Backwards");
                    break;
                case "Backwards":
                    setConfig("Rotation", "Hypixel");
                    break;
                case "Hypixel":
                    setConfig("Rotation", "None");
                    break;
                default:
                    setConfig("Rotation", "None");
            }
        } else if (key.equals("KeepY")) {
            String currentRotation = (String) getConfig("KeepY");
            switch (currentRotation) {
                case "None":
                    setConfig("KeepY", "Vanilla");
                    break;
                case "Vanilla":
                    setConfig("KeepY", "Extra");
                    break;
                case "Extra":
                    setConfig("KeepY", "Telly");
                    break;
                case "Telly":
                    setConfig("KeepY", "None");
                    break;
                default:
                    setConfig("KeepY", "None");
            }
        } else if (key.equals("MoveFix")) {
            String currentMoveFix = (String) getConfig("MoveFix");
            switch (currentMoveFix) {
                case "None":
                    setConfig("MoveFix", "Silent");
                    break;
                case "Silent":
                    setConfig("MoveFix", "None");
                    break;
                default:
                    setConfig("MoveFix", "None");
            }
        } else if (key.equals("Sprint")) {
            String currentSprint = (String) getConfig("Sprint");
            switch (currentSprint) {
                case "None":
                    setConfig("Sprint", "Vanilla");
                    break;
                case "Vanilla":
                    setConfig("Sprint", "None");
                    break;
                default:
                    setConfig("Sprint", "None");
            }
        }
    }

    private static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing enumFacing) {
            this.blockPos = blockPos;
            this.facing = enumFacing;
        }

        public BlockPos blockPos() {
            return this.blockPos;
        }

        public EnumFacing facing() {
            return this.facing;
        }
    }
}