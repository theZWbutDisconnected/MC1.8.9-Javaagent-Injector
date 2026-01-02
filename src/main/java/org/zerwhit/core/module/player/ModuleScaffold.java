package org.zerwhit.core.module.player;

import javafx.scene.input.KeyCode;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldSettings;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.manager.RotationManager;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.BlockUtil;
import org.zerwhit.core.util.KeyBindUtil;
import org.zerwhit.core.util.RandomUtil;

import java.util.ArrayList;
import java.util.Comparator;

public class ModuleScaffold extends ModuleBase implements ITickableModule {
    private int stage;
    private double startY;
    private int placeTick;
    private int blockCount;
    float pitch, yaw;

    public ModuleScaffold() {
        super("Scaffold", false, "Movement");
        addConfig("Sprint", false);
    }


    @Override
    public void onModuleTick() {
        updateBlockCount();
        if (isRequireMove()) {
            if (Meta.slientAimEnabled) {
                mc.thePlayer.rotationPitch = pitch;
                mc.thePlayer.rotationYaw = 180 + rotMng.rendererViewEntity.rotationYaw;
            }
            mc.thePlayer.rotationPitch += RotationManager.wrapAngleDiff(pitch, mc.thePlayer.rotationPitch) * 0.2f;
            mc.thePlayer.rotationYaw += RotationManager.wrapAngleDiff(yaw, mc.thePlayer.rotationYaw) * 0.2f;
            if (stage == 0) {
                startY = mc.thePlayer.posY;
                stage = 1;
            }
            if (stage == 1) {
                Meta.slientAimEnabled = true;
                Meta.strafeEnabled = true;
                BlockData bd = getBlockData();
                if (bd != null) {
                    double deltaX = bd.blockPos().getX() - mc.thePlayer.posX;
                    double deltaY = (bd.blockPos().getY() + 1) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
                    double deltaZ = bd.blockPos().getZ() - mc.thePlayer.posZ;
                    double dis = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);

                    pitch = (float) (-(Math.atan2(deltaY, dis) * 180.0 / Math.PI));
                    yaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI);
                    if (placeTick <= 0) {
                        place(bd.blockPos, bd.facing, BlockUtil.getHitVec(bd.blockPos, bd.facing, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), true);
                    }
                }
            }
        } else {
            Meta.slientAimEnabled = false;
            Meta.strafeEnabled = false;
            stage = 0;
        }
        placeTick--;
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

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3, boolean swing) {
        if (BlockUtil.isHoldingBlock() && this.blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), blockPos, enumFacing, vec3)) {
                if (mc.playerController.getCurrentGameType() != WorldSettings.GameType.CREATIVE) {
                    this.blockCount--;
                }
                placeTick = 7 + RandomUtil.nextInt(0, 4);
                if (swing) {
                    mc.thePlayer.swingItem();
                } else {
                    mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
                }
            }
        }
    }

    private boolean isRequireMove() {
        return KeyBindUtil.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()) || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()) || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
    }

    private BlockData getBlockData() {
        int currentY = MathHelper.floor_double(mc.thePlayer.posY);
        int targetY = currentY - 1;

        if (this.stage != 0) {
            targetY = (int) (this.startY - 1);
        }

        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                targetY,
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
                            (this.stage == 0 || pos.getY() <= this.startY)) {

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
