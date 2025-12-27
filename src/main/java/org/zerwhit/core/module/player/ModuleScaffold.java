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
import org.zerwhit.core.util.BlockUtil;
import org.zerwhit.core.util.ItemUtil;
import org.zerwhit.core.util.KeyRobot;
import org.zerwhit.core.util.RandomUtil;

import java.util.ArrayList;
import java.util.Comparator;

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
    
    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerDelay = 0;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private EnumFacing targetFacing = null;
    
    public ModuleScaffold() {
        super("Scaffold", false, "Movement");
        addConfig("Mode", "Telly");
        addConfig("Rotation", "Vanilla");
        addConfig("Tower", false);
        addConfig("MultiPlace", false);
        addConfig("KeepY", "Vanilla");
        setBindingKey(KeyCode.C);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.rotationTick = 0;
        this.lastSlot = -1;
        this.blockCount = -1;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.stage = 0;
        this.startY = 256;
        this.shouldKeepY = false;
        this.towering = false;
        this.targetFacing = null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Meta.slientAimEnabled = false;
    }

    @Override
    public void onModuleTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        String mode = (String) getConfig("Mode");

        Meta.slientAimEnabled = true;
        if (mode.equals("Telly")) {
            handleTellyMode();
        } else if (mode.equals("Clutch")) {
        } else {
        }
    }
    
    private void handleTellyMode() {
        if (this.rotationTick > 0) {
            this.rotationTick--;
        }
        if (mc.thePlayer.onGround) {
            if (this.stage > 0) {
                this.stage--;
            }
            if (this.stage < 0) {
                this.stage++;
            }
            if (this.stage == 0 && (Boolean) getConfig("Tower") && !mc.gameSettings.keyBindJump.isKeyDown()) {
                this.stage = 1;
            }
            this.startY = this.shouldKeepY ? this.startY : MathHelper.floor_double(mc.thePlayer.posY);
            this.shouldKeepY = false;
            this.towering = false;
        }

        if (this.canPlace()) {
            ItemStack stack = mc.thePlayer.getHeldItem();
            int count = ItemUtil.isBlock(stack) ? stack.stackSize : 0;
            this.blockCount = Math.min(this.blockCount, count);
            if (this.blockCount <= 0) {
                for (int i = 0; i < 9; i++) {
                    ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(i);
                    if (ItemUtil.isBlock(candidate)) {
                        mc.thePlayer.inventory.currentItem = i;
                        this.blockCount = candidate.stackSize;
                        break;
                    }
                }
            }
            float currentYaw = this.getCurrentYaw();
            float yawDiffTo180 = RotationManager.wrapAngleDiff(currentYaw - 180.0F, mc.thePlayer.rotationYaw);
            float diagonalYaw = this.isDiagonal(currentYaw)
                    ? yawDiffTo180
                    : RotationManager.wrapAngleDiff(currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), mc.thePlayer.rotationYaw);
            if (!this.canRotate) {
                switch ((String) getConfig("Rotation")) {
                    case "Vanilla":
                        if (this.yaw == -180.0F && this.pitch == 0.0F) {
                            this.yaw = RotationManager.quantizeAngle(diagonalYaw);
                            this.pitch = RotationManager.quantizeAngle(85.0F);
                        } else {
                            this.yaw = RotationManager.quantizeAngle(diagonalYaw);
                        }
                        break;
                    case "Backwards":
                        if (this.yaw == -180.0F && this.pitch == 0.0F) {
                            this.yaw = RotationManager.quantizeAngle(yawDiffTo180);
                            this.pitch = RotationManager.quantizeAngle(85.0F);
                        } else {
                            this.yaw = RotationManager.quantizeAngle(yawDiffTo180);
                        }
                        break;
                    case "Hypixel":
                        if (this.yaw == -180.0F && this.pitch == 0.0F) {
                            this.yaw = RotationManager.quantizeAngle(diagonalYaw);
                            this.pitch = RotationManager.quantizeAngle(85.0F);
                        } else {
                            this.yaw = RotationManager.quantizeAngle(diagonalYaw);
                        }
                }
            }
            BlockData blockData = this.getBlockData();
            Vec3 hitVec = null;
            if (blockData != null) {
                double[] x = placeOffsets;
                double[] y = placeOffsets;
                double[] z = placeOffsets;
                switch (blockData.facing()) {
                    case NORTH:
                        z = new double[]{0.0};
                        break;
                    case EAST:
                        x = new double[]{1.0};
                        break;
                    case SOUTH:
                        z = new double[]{1.0};
                        break;
                    case WEST:
                        x = new double[]{0.0};
                        break;
                    case DOWN:
                        y = new double[]{0.0};
                        break;
                    case UP:
                        y = new double[]{1.0};
                }
                float bestYaw = -180.0F;
                float bestPitch = 0.0F;
                float bestDiff = 0.0F;
                for (double dx : x) {
                    for (double dy : y) {
                        for (double dz : z) {
                            double relX = (double) blockData.blockPos().getX() + dx - mc.thePlayer.posX;
                            double relY = (double) blockData.blockPos().getY() + dy - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                            double relZ = (double) blockData.blockPos().getZ() + dz - mc.thePlayer.posZ;
                            float baseYaw = RotationManager.wrapAngleDiff(this.yaw, mc.thePlayer.rotationYaw);
                            float[] rotations = RotationManager.getRotationsTo(relX, relY, relZ, baseYaw, this.pitch);
                            MovingObjectPosition mop = RotationManager.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F, mc.thePlayer);
                            if (mop != null
                                    && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                                    && mop.getBlockPos().equals(blockData.blockPos())
                                    && mop.sideHit == blockData.facing()) {
                                float totalDiff = Math.abs(rotations[0] - baseYaw) + Math.abs(rotations[1] - this.pitch);
                                if (bestYaw == -180.0F && bestPitch == 0.0F || totalDiff < bestDiff) {
                                    bestYaw = rotations[0];
                                    bestPitch = rotations[1];
                                    bestDiff = totalDiff;
                                    hitVec = mop.hitVec;
                                }
                            }
                        }
                    }
                }
                if (bestYaw != -180.0F || bestPitch != 0.0F) {
                    this.yaw = bestYaw;
                    this.pitch = bestPitch;
                    this.canRotate = true;
                }
            }
            if (this.canRotate && mc.gameSettings.keyBindForward.isKeyDown() && Math.abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - this.yaw)) < 90.0F) {
                switch ((String) getConfig("Rotation")) {
                    case "Backwards":
                        this.yaw = RotationManager.quantizeAngle(yawDiffTo180);
                        break;
                    case "Hypixel":
                        this.yaw = RotationManager.quantizeAngle(diagonalYaw);
                }
            }
            if (getConfig("Rotation") != "None") {
                float targetYaw = this.yaw;
                float targetPitch = this.pitch;
                if (this.towering && (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (double) (this.startY + 1))) {
                    float yawDiff = MathHelper.wrapAngleTo180_float(this.yaw - mc.thePlayer.rotationYaw);
                    float tolerance = this.rotationTick >= 2 ? RandomUtil.nextFloat(90.0F, 95.0F) : RandomUtil.nextFloat(30.0F, 35.0F);
                    if (Math.abs(yawDiff) > tolerance) {
                        float clampedYaw = RotationManager.clampAngle(yawDiff, tolerance);
                        targetYaw = RotationManager.quantizeAngle(mc.thePlayer.rotationYaw + clampedYaw);
                        this.rotationTick = Math.max(this.rotationTick, 1);
                    }
                }
                /*if (this.isTowering()) {
                    float yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - mc.thePlayer.prevRotationYaw);
                    targetYaw = RotationManager.quantizeAngle(mc.thePlayer.rotationYaw + yawDelta * RandomUtil.nextFloat(0.98F, 0.99F));
                    targetPitch = RotationManager.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F));
                    this.rotationTick = 3;
                    this.towering = true;
                }*/
                mc.thePlayer.rotationYaw += (targetYaw - mc.thePlayer.rotationYaw) * 0.2f;
                mc.thePlayer.rotationPitch += rotMng.calculateSmoothPitchChange(mc.thePlayer.rotationPitch, targetPitch);
//                if (this.moveFix.getValue() == 1) {
//                    event.setPervRotation(targetYaw, 3);
//                }
            }
            if (blockData != null && hitVec != null && this.rotationTick <= 0) {
                this.place(blockData.blockPos(), blockData.facing(), hitVec);
                if ((Boolean) getConfig("MultiPlace")) {
                    for (int i = 0; i < 3; i++) {
                        blockData = this.getBlockData();
                        if (blockData == null) {
                            break;
                        }
                        MovingObjectPosition mop = RotationManager.rayTrace(this.yaw, this.pitch, mc.playerController.getBlockReachDistance(), 1.0F, mc.thePlayer);
                        if (mop != null
                                && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                                && mop.getBlockPos().equals(blockData.blockPos())
                                && mop.sideHit == blockData.facing()) {
                            this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                        } else {
                            hitVec = BlockUtil.getClickVec(blockData.blockPos(), blockData.facing());
                            double dx = hitVec.xCoord - mc.thePlayer.posX;
                            double dy = hitVec.yCoord - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                            double dz = hitVec.zCoord - mc.thePlayer.posZ;
                            float[] rotations = RotationManager.getRotationsTo(dx, dy, dz, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                            if (!(Math.abs(rotations[0] - this.yaw) < 120.0F) || !(Math.abs(rotations[1] - this.pitch) < 60.0F)) {
                                break;
                            }
                            mop = RotationManager.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F, mc.thePlayer);
                            if (mop == null
                                    || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                                    || !mop.getBlockPos().equals(blockData.blockPos())
                                    || mop.sideHit != blockData.facing()) {
                                break;
                            }
                            this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                        }
                    }
                }
            }
            if (this.targetFacing != null) {
                if (this.rotationTick <= 0) {
                    int playerBlockX = MathHelper.floor_double(mc.thePlayer.posX);
                    int playerBlockY = MathHelper.floor_double(mc.thePlayer.posY);
                    int playerBlockZ = MathHelper.floor_double(mc.thePlayer.posZ);
                    BlockPos belowPlayer = new BlockPos(playerBlockX, playerBlockY - 1, playerBlockZ);
                    hitVec = BlockUtil.getHitVec(belowPlayer, this.targetFacing, this.yaw, this.pitch);
                    this.place(belowPlayer, this.targetFacing, hitVec);
                }
                this.targetFacing = null;
            } else if ((String) getConfig("KeepY") == "Extra" && this.stage > 0 && !mc.thePlayer.onGround) {
                int nextBlockY = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.motionY);
                if (nextBlockY <= this.startY && mc.thePlayer.posY > (double) (this.startY + 1)) {
                    this.shouldKeepY = true;
                    blockData = this.getBlockData();
                    if (blockData != null && this.rotationTick <= 0) {
                        hitVec = BlockUtil.getHitVec(blockData.blockPos(), blockData.facing(), this.yaw, this.pitch);
                        this.place(blockData.blockPos(), blockData.facing(), hitVec);
                    }
                }
            }
        }
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }
    
    private boolean canPlace() {
        return mc.thePlayer != null && mc.theWorld != null && !mc.thePlayer.isDead;
    }
    
    private void updateBlockCount() {
        ItemStack stack = mc.thePlayer.getHeldItem();
        int count = isBlock(stack) ? stack.stackSize : 0;
        this.blockCount = Math.min(this.blockCount, count);
        
        if (this.blockCount <= 0) {
            for (int i = 0; i < 9; i++) {
                ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(i);
                if (isBlock(candidate)) {
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
        
        if (!isReplaceable(targetPos)) {
            return null;
        }
        
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 0; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    if (!isReplaceable(pos) && !isContainer(pos) && 
                        mc.thePlayer.getDistance((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 
                        (double) mc.playerController.getBlockReachDistance() && 
                        (this.stage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {
                        
                        for (EnumFacing facing : EnumFacing.VALUES) {
                            if (facing != EnumFacing.DOWN) {
                                BlockPos blockPos = pos.offset(facing);
                                if (isReplaceable(blockPos)) {
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
    
    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (isHoldingBlock() && this.blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), blockPos, enumFacing, vec3)) {
                if (mc.playerController.getCurrentGameType() != WorldSettings.GameType.CREATIVE) {
                    this.blockCount--;
                }
                mc.thePlayer.swingItem();
            }
        }
    }
    
    private boolean isHoldingBlock() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        return heldItem != null && heldItem.getItem() instanceof ItemBlock;
    }
    
    private boolean isBlock(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemBlock;
    }
    
    private boolean isReplaceable(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.isAir(mc.theWorld, pos) || block.isReplaceable(mc.theWorld, pos);
    }
    
    private boolean isContainer(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.hasTileEntity(mc.theWorld.getBlockState(pos));
    }

    private float getCurrentYaw() {
        return mc.thePlayer.rotationYaw;
    }

    private float getCurrentPitch() {
        return mc.thePlayer.rotationPitch;
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
            String currentRotation = (String) getConfig("Rotation");
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
        }
    }
    
    @Override
    public double getMaxValueForConfig(String key) {
        if (key.equals("Speed")) {
            return 2.0;
        }
        return super.getMaxValueForConfig(key);
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
