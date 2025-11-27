package com.zerwhit.core.module.visual;

import com.zerwhit.core.Meta;
import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public class ModuleFreeLook extends ModuleBase implements ITickableModule {
    
    public ModuleFreeLook() {
        super("FreeLook", false, "Visual");
    }

    @Override
    public void onEnable() {
        Meta.slientAimEnabled = true;
    }

    @Override
    public void onDisable() {
        Meta.slientAimEnabled = false;
    }

    @Override
    public void onModuleTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        Meta.slientAimEnabled = true;
    }
}