package org.zerwhit.core.module.combat;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.WorldSettings;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.ItemUtil;
import org.zerwhit.core.util.KeyBindUtil;
import org.zerwhit.core.util.RandomUtil;

public class ModuleAutoClicker extends ModuleBase implements ITickableModule {
    private boolean clickPending = false;
    private long nextClickTime = 0L;
    private boolean blockHitPending = false;
    private long nextBlockHitTime = 0L;

    public ModuleAutoClicker() {
        super("AutoClicker", true, "Combat");
        addRangedConfig("CPSMax", 15, 0, 20);
        addRangedConfig("CPSMin", 10, 0, 20);
        addRangedConfig("BlockHitTicks", 1.5F, 1.0F, 20.0F);
        addConfig("WeaponsOnly", false);
        addConfig("AllowTools", false);
        addConfig("BreakBlocks", true);
        addConfig("BlockHit", true);
    }

    private long getNextClickDelay() {
        int cps = RandomUtil.nextInt(((Number) getConfig("CPSMin")).intValue(),
                ((Number) getConfig("CPSMax")).intValue());
        if (cps <= 0) return Long.MAX_VALUE;
        return 1000L / cps + RandomUtil.nextLong(-50, 50);
    }

    private long getBlockHitDelay() {
        return (long) (50.0F * ((Number) getConfig("BlockHitTicks")).floatValue());
    }

    private boolean canClick() {
        if (!(Boolean) getConfig("WeaponsOnly")
                || ItemUtil.hasRawUnbreakingEnchant()
                || (Boolean) getConfig("AllowTools") && ItemUtil.isHoldingTool()) {
            if ((Boolean) getConfig("BreakBlocks") && this.isBreakingBlock()) {
                WorldSettings.GameType gameType = mc.playerController.getCurrentGameType();
                return gameType != WorldSettings.GameType.SURVIVAL && gameType != WorldSettings.GameType.CREATIVE;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private boolean isBreakingBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    @Override
    public void onModuleTick() {
        long currentTime = System.currentTimeMillis();

        if (mc.currentScreen != null) {
            this.clickPending = false;
            this.blockHitPending = false;
        } else {
            if (this.clickPending) {
                this.clickPending = false;
                KeyBindUtil.updateKeyState(mc.gameSettings.keyBindAttack.getKeyCode());
            }

            if (this.blockHitPending) {
                this.blockHitPending = false;
                KeyBindUtil.updateKeyState(mc.gameSettings.keyBindUseItem.getKeyCode());
            }

            if (this.isEnabled() && this.canClick() && mc.gameSettings.keyBindAttack.isKeyDown()) {
                if (!mc.thePlayer.isUsingItem() && currentTime >= this.nextClickTime) {
                    this.clickPending = true;
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                    KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindAttack.getKeyCode());

                    this.nextClickTime = currentTime + this.getNextClickDelay();
                }

                if ((Boolean) getConfig("BlockHit")
                        && currentTime >= this.nextBlockHitTime
                        && mc.gameSettings.keyBindUseItem.isKeyDown()
                        && ItemUtil.isHoldingSword()) {
                    if (!mc.thePlayer.isUsingItem()) {
                        this.blockHitPending = true;
                        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                        KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindUseItem.getKeyCode());

                        this.nextBlockHitTime = currentTime + this.getBlockHitDelay();
                    }
                }
            } else if (!mc.gameSettings.keyBindAttack.isKeyDown()) {
                this.nextClickTime = 0L;
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.nextClickTime = 0L;
        this.nextBlockHitTime = 0L;
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