package com.zerwhit.core.module.combat;

import com.zerwhit.core.module.Module;
import org.lwjgl.input.Mouse;

public class ModuleAutoClicker extends Module {
    private long lastClickTime = 0;
    
    public ModuleAutoClicker() {
        super("AutoClicker", false, "Combat");
        addConfig("CPS", 12);
        addConfig("LeftClick", true);
        addConfig("RightClick", false);
    }

    @Override
    public void onModuleTick() {
        int cps = (Integer) getConfig("CPS");
        boolean leftClick = (Boolean) getConfig("LeftClick");
        boolean rightClick = (Boolean) getConfig("RightClick");
        
        long delay = 1000 / cps;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastClickTime >= delay) {
            if (leftClick && Mouse.isButtonDown(0)) {
                lastClickTime = currentTime;
            } else if (rightClick && Mouse.isButtonDown(1)) {
                lastClickTime = currentTime;
            }
        }
    }
}