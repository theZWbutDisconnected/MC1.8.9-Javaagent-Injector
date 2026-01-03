package org.zerwhit.core.module.combat;

import javafx.scene.input.KeyCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.zerwhit.core.Hooks;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.manager.RotationManager;
import org.zerwhit.core.module.IRenderModule;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.KeyBindUtil;
import org.zerwhit.core.util.KeyRobot;
import org.zerwhit.core.util.ObfuscationReflectionHelper;
import org.zerwhit.core.util.RandomUtil;

import java.util.List;
import java.util.Random;

public class ModuleSlientAura extends ModuleBase implements ITickableModule, IRenderModule {
    private Entity target = null;
    private int lastAttackTick;
    private Random rand = new Random();

    float newYaw, newPitch;
    private int blockTime;
    private long nextClickTime;

    public ModuleSlientAura() {
        super("SlientAura", true, "Combat", KeyCode.TAB);
        addRangedConfig("CPSMax", 15, 0, 20);
        addRangedConfig("CPSMin", 10, 0, 20);
        addConfig("Distance", 4.0f);
        addConfig("SlientAim", true);
        addConfig("PitchEnabled", true);
        addConfig("AutoBlock", true);
    }

    private long getNextClickDelay() {
        int cps = RandomUtil.nextInt(((Number) getConfig("CPSMin")).intValue(),
                ((Number) getConfig("CPSMax")).intValue());
        if (cps <= 0) return Long.MAX_VALUE;
        return 1000L / cps + RandomUtil.nextLong(-50, 50);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Meta.slientAimEnabled = false;
        Meta.blockRenderEnabled = false;
        Meta.strafeEnabled = false;
    }

    @Override
    public void onModuleTick() {
        Float distance = (Float) getConfig("Distance");
        Boolean slient = (Boolean) getConfig("SlientAim");
        Boolean pitchEnabled = (Boolean) getConfig("PitchEnabled");
        Boolean autoBlock = (Boolean) getConfig("AutoBlock");

        Meta.blockRenderEnabled = false;
        Meta.strafeEnabled = false;
        if (ModuleBase.scaffold.enabled) return;
        if (!frelook.enabled)
            Meta.slientAimEnabled = false;
        List<Entity> entityList = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().expand(distance, distance, distance));
        if (!entityList.isEmpty()) {
            Meta.slientAimEnabled = slient;
            if (slient)
                Meta.strafeEnabled = true;
        }
        for (int i = 0; i < entityList.size(); i++) {
            Entity entity = entityList.get(i);
            if (!(entity instanceof EntityPlayer)) {
                target = null;
                continue;
            }
            if (!(entity.isEntityAlive() && !entity.isDead)) {
                if (target == entity)
                    target = null;
                continue;
            }
            if (target == null || target.getDistanceToEntity(mc.thePlayer) > entity.getDistanceToEntity(mc.thePlayer)) target = entity;
        }

        if (target != null && target.getDistanceToEntity(mc.thePlayer) > distance) {
            target = null;
        }

        if (target == null && !frelook.enabled) {
            Meta.slientAimEnabled = false;
            Meta.strafeEnabled = false;
            return;
        }
        Meta.blockRenderEnabled = autoBlock;
        
        double deltaX = target.posX - mc.thePlayer.posX;
        double deltaY = (target.posY + 1) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = target.posZ - mc.thePlayer.posZ;
        double dis = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);

        double targetPitch = (float) (-(Math.atan2(deltaY, dis) * 180.0 / Math.PI) + Math.sin(System.nanoTime() / 70_000_000.0) * 15);
        double targetYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;

        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;

        float yawDiff = (float) (targetYaw - currentYaw);
        yawDiff = rotMng.normalizeAngle(yawDiff);

        boolean shouldReverseYaw = rotMng.isViewExceedingBoundingBox(currentYaw, target, mc.thePlayer);
        
        if (MathHelper.abs(yawDiff) > 10 + rand.nextFloat() * 5) {
            if (shouldReverseYaw) {
                newYaw = currentYaw - yawDiff * 1.2f + RandomUtil.nextFloat(-10, 10);
            } else {
                newYaw = currentYaw + yawDiff;
            }
        }
        
        if (!shouldReverseYaw) {
            mc.thePlayer.rotationYaw = currentYaw + RotationManager.wrapAngleDiff(newYaw, currentYaw) * 0.07f;
        }
        
        if (pitchEnabled) {
            float pitchDiff = (float) (targetPitch - currentPitch);
            pitchDiff = Math.max(-90, Math.min(90, pitchDiff));

            newPitch = currentPitch + pitchDiff;
            newPitch = Math.max(-90, Math.min(90, newPitch));

            float smoothPitchChange = rotMng.calculateSmoothPitchChange(currentPitch, newPitch);
            mc.thePlayer.rotationPitch = Math.max(-90, Math.min(90, newPitch + smoothPitchChange));
        }

        handleClick();
    }

    private void handleClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime < nextClickTime) { return;}
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
        if ((boolean) getConfig("AutoBlock")) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            KeyBindUtil.updateKeyState(mc.gameSettings.keyBindUseItem.getKeyCode());
            blockTime = 5;
        }
        nextClickTime = currentTime + getNextClickDelay();
        blockTime--;
        if (blockTime <= 0) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            KeyBindUtil.updateKeyState(mc.gameSettings.keyBindUseItem.getKeyCode());
        }
    }

    @Override
    public void onRender(float partialTicks, int screenWidth, int screenHeight) {
        if (target != null) {
            
        }
    }
}
