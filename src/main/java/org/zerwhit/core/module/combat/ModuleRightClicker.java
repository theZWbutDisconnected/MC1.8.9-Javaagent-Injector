package org.zerwhit.core.module.combat;

import net.minecraft.item.ItemFood;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldSettings;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.ItemUtil;
import org.zerwhit.core.util.KeyBindUtil;
import org.zerwhit.core.util.RandomUtil;

public class ModuleRightClicker extends ModuleBase implements ITickableModule {
    private boolean clickPending = false;
    private long nextClickTime = 0L;

    public ModuleRightClicker() {
        super("RightClicker", false, "Combat");
        addRangedConfig("CPSMax", 15, 0, 20);
        addRangedConfig("CPSMin", 10, 0, 20);
    }

    private long getNextClickDelay() {
        int cps = RandomUtil.nextInt(((Number) getConfig("CPSMin")).intValue(),
                ((Number) getConfig("CPSMax")).intValue());
        if (cps <= 0) return Long.MAX_VALUE;
        return 1000L / cps + RandomUtil.nextLong(-50, 50);
    }

    @Override
    public void onModuleTick() {
        long currentTime = System.currentTimeMillis();
        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemFood) return;
        if (mc.currentScreen != null) {
            this.clickPending = false;
        } else {
            if (this.clickPending) {
                this.clickPending = false;
                KeyBindUtil.updateKeyState(mc.gameSettings.keyBindUseItem.getKeyCode());
            }

            if (mc.gameSettings.keyBindUseItem.isKeyDown()) {
                if (currentTime >= this.nextClickTime) {
                    this.clickPending = true;
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindUseItem.getKeyCode());

                    this.nextClickTime = currentTime + this.getNextClickDelay();
                }
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.nextClickTime = 0L;
    }

    @Override
    public double getMaxValueForConfig(String key) {
        if (key.equals("CPSMin")) return (Integer) getConfig("CPSMax");
        return super.getMaxValueForConfig(key);
    }

    @Override
    public double getMinValueForConfig(String key) {
        if (key.equals("CPSMax")) return (Integer) getConfig("CPSMin");
        return super.getMinValueForConfig(key);
    }
}