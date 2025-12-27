package org.zerwhit.core.module.movement;

import net.minecraft.client.settings.KeyBinding;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.module.ToggleMode;
import javafx.scene.input.KeyCode;
import net.minecraft.network.play.client.C0BPacketEntityAction;

public class ModuleSprint extends ModuleBase implements ITickableModule {
    public ModuleSprint() {
        super("Sprint", true, "Movement");
        addConfig("OmniDirectional", false);
        addConfig("Mode", "Legit");
    }

    @Override
    public void onModuleTick() {
        boolean omniDirectional = (Boolean) getConfig("OmniDirectional");
        String mode = (String) getConfig("Mode");
        
        switch (mode) {
            case "Legit":
                if (omniDirectional) {
                    if (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                    }
                } else {
                    if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isCollidedHorizontally) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                    }
                }
                break;
            case "Rage":
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                break;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
    }
    
    @Override
    public void cycleStringConfig(String key) {
        if (key.equals("Mode")) {
            String currentMode = (String) getConfig("Mode");
            switch (currentMode) {
                case "Legit":
                    setConfig("Mode", "Rage");
                    break;
                case "Rage":
                    setConfig("Mode", "Legit");
                    break;
                default:
                    setConfig("Mode", "Legit");
            }
        }
    }
}