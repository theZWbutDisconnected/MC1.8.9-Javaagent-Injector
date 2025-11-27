package com.zerwhit.core.module.visual;

import com.zerwhit.core.Meta;
import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class ModuleFreeLook extends ModuleBase implements ITickableModule {
    public boolean pressed;
    public boolean slient;
    public int presonView;
    public ModuleFreeLook() {
        super("FreeLook", true, "Visual");
    }

    @Override
    public void onEnable() {
        Meta.slientAimEnabled = true;
    }

    @Override
    public void onDisable() {
        Meta.slientAimEnabled = false;
        pressed = false;
    }

    @Override
    public void onModuleTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (Mouse.isButtonDown(2)) {
            if (pressed) return;
            slient = Meta.slientAimEnabled;
            presonView = mc.gameSettings.thirdPersonView;
            pressed = true;
            Meta.slientAimEnabled = true;
            mc.gameSettings.thirdPersonView = 2;
        } else if (pressed) {
            Meta.slientAimEnabled = slient;
            mc.gameSettings.thirdPersonView = presonView;
            if (!slient) {
                rotationManager.rendererViewEntity.rotationPitch = mc.thePlayer.rotationPitch;
                rotationManager.rendererViewEntity.rotationYaw = mc.thePlayer.rotationYaw;
                rotationManager.rendererViewEntity.prevRotationPitch = mc.thePlayer.rotationPitch;
                rotationManager.rendererViewEntity.prevRotationYaw = mc.thePlayer.rotationYaw;
            }
            pressed = false;
        }
    }
}