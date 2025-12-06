package com.zerwhit.core.module.combat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;
import com.zerwhit.core.util.KeyRobot;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.event.InputEvent;

public class ModuleAutoClicker extends ModuleBase implements ITickableModule {
    private static final Logger logger = LogManager.getLogger(ModuleAutoClicker.class);
    
    private long lastClickTime = 0;
    
    public ModuleAutoClicker() {
        super("AutoClicker", false, "Combat");
        addConfig("CPS", 12);
        addConfig("LeftClick", true);
        addConfig("RightClick", false);
        addConfig("RequireMouseDown", true);
        addConfig("Randomization", 10);
    }

    @Override
    public void onModuleTick() {
        int cps = (Integer) getConfig("CPS");
        boolean leftClick = (Boolean) getConfig("LeftClick");
        boolean rightClick = (Boolean) getConfig("RightClick");
        boolean requireMouseDown = (Boolean) getConfig("RequireMouseDown");
        int randomization = (Integer) getConfig("Randomization");
        
        long delay = calculateDelay(cps, randomization);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastClickTime >= delay) {
            if (leftClick && (!requireMouseDown || Mouse.isButtonDown(0))) {
                boolean isBlocking = mc.thePlayer.isBlocking();
                performClick(InputEvent.BUTTON1_DOWN_MASK);
                lastClickTime = currentTime;
            } else if (rightClick && (!requireMouseDown || Mouse.isButtonDown(1))) {
                performClick(InputEvent.BUTTON3_DOWN_MASK);
                lastClickTime = currentTime;
            }
        }
    }
    
    private long calculateDelay(int cps, int randomization) {
        if (cps <= 0) return 1000;
        long baseDelay = 1000 / cps;
        int randomOffset = (int) (Math.random() * randomization * 2) - randomization;
        return Math.max(50, baseDelay + randomOffset);
    }
    
    private void performClick(int buttonMask) {
        KeyRobot.clickMouse(buttonMask);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            logger.error("Thread interrupted during click:", e);
        }
        KeyRobot.releaseMouse(buttonMask);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
    }
    
    @Override
    public double getMaxValueForConfig(String key) {
        if ("CPS".equals(key)) {
            return 20.0;
        } else if ("Randomization".equals(key)) {
            return 50.0;
        }
        return super.getMaxValueForConfig(key);
    }
}