package org.zerwhit.core.module.visual;

import javafx.scene.input.KeyCode;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.lwjgl.input.Mouse;
import org.zerwhit.core.module.ToggleMode;

public class ModuleFreeLook extends ModuleBase implements ITickableModule {
    public ModuleFreeLook(KeyCode bdkey) {
        super("FreeLook", false, "Visual", bdkey, ToggleMode.HOLD);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Meta.slientAimEnabled = true;
        mc.gameSettings.thirdPersonView = 1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Meta.slientAimEnabled = false;
        mc.gameSettings.thirdPersonView = 0;
    }

    @Override
    public void onModuleTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        Meta.slientAimEnabled = true;
    }
}