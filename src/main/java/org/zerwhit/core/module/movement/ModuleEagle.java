package org.zerwhit.core.module.movement;

import javafx.scene.input.KeyCode;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.RandomUtils;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.BlockUtil;
import org.zerwhit.core.util.KeyBindUtil;
import org.zerwhit.core.util.ObfuscationReflectionHelper;

import java.util.Objects;

public class ModuleEagle extends ModuleBase implements ITickableModule {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int sneakDelay = 0;
    private boolean sneaked;

    private boolean canMoveSafely() {
        double[] offset = predictMovement();
        return canMove(mc.thePlayer.motionX + offset[0], mc.thePlayer.motionZ + offset[1]);
    }

    public static double[] predictMovement() {
        float strafeInput = (float) getLeftValue() * 0.98f;
        float forwardInput = (float) getForwardValue() * 0.98f;
        float inputMagnitude = strafeInput * strafeInput + forwardInput * forwardInput;
        if (inputMagnitude >= 1.0E-4f) {
            inputMagnitude = MathHelper.sqrt_float(inputMagnitude);
            if (inputMagnitude < 1.0f) {
                inputMagnitude = 1.0f;
            }
            inputMagnitude = getAllowedHorizontalDistance() / inputMagnitude;
            float sinYaw = MathHelper.sin(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            float cosYaw = MathHelper.cos(mc.thePlayer.rotationYaw * (float) Math.PI / 180.0f);
            strafeInput *= inputMagnitude;
            forwardInput *= inputMagnitude;
            return new double[]{strafeInput * cosYaw - forwardInput * sinYaw, forwardInput * cosYaw + strafeInput * sinYaw};
        }
        return new double[]{0.0, 0.0};
    }

    public static float getAllowedHorizontalDistance() {
        float slipperiness = mc.thePlayer.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.getEntityBoundingBox().minY) - 1, MathHelper.floor_double(mc.thePlayer.posZ))).getBlock().slipperiness * 0.91f;
        return mc.thePlayer.getAIMoveSpeed() * (0.16277136f / (slipperiness * slipperiness * slipperiness));
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (mc.gameSettings.keyBindLeft.isKeyDown()) {
            ++leftValue;
        }
        if (mc.gameSettings.keyBindRight.isKeyDown()) {
            --leftValue;
        }
        return leftValue;
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (mc.gameSettings.keyBindForward.isKeyDown()) {
            ++forwardValue;
        }
        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            --forwardValue;
        }
        return forwardValue;
    }

    private boolean shouldSneak() {
        if ((Boolean) getConfig("DirectionCheck") && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if ((Boolean) getConfig("PitchCheck") && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else {
            return (!(Boolean) getConfig("BlocksOnly") || BlockUtil.isHoldingBlock()) && mc.thePlayer.onGround;
        }
    }

    public static boolean canMove(double x, double z) {
        return canMove(x, z, -1.0);
    }

    public static boolean canMove(double x, double z, double y) {
        AxisAlignedBB boundingBox = mc.thePlayer.getEntityBoundingBox().offset(x, y, z);
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, boundingBox).isEmpty();
    }

    public ModuleEagle() {
        super("Eagle", true, "Movement");
        addRangedConfig("MinDelay", 2, 0, 10);
        addRangedConfig("MaxDelay", 3, 0, 10);
        addConfig("DirectionCheck", true);
        addConfig("PitchCheck", false);
        addConfig("BlocksOnly", true);
    }

    public void onModuleTick() {
        if (this.sneakDelay > 0) {
            this.sneakDelay--;
        }
        if (this.sneakDelay == 0 && this.canMoveSafely()) {
            int minDelay = ((Number) getConfig("MinDelay")).intValue();
            int maxDelay = ((Number) getConfig("MaxDelay")).intValue();
            this.sneakDelay = RandomUtils.nextInt(minDelay, maxDelay + 1);
        } else {
            if (sneaked)
                KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            sneaked = false;
        }
        if (this.shouldSneak() && (this.sneakDelay > 0 || this.canMoveSafely())) {
            sneaked = true;
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        }
    }

    @Override
    public void onDisable() {
        this.sneakDelay = 0;
    }

    @Override
    public double getMaxValueForConfig(String key) {
        if (key.equals("MinDelay")) return ((Number) getConfig("MaxDelay")).doubleValue();
        return super.getMaxValueForConfig(key);
    }

    @Override
    public double getMinValueForConfig(String key) {
        if (key.equals("MaxDelay")) return ((Number) getConfig("MinDelay")).doubleValue();
        return super.getMinValueForConfig(key);
    }
}
