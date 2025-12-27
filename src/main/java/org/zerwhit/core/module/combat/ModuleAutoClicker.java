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
    private long clickDelay = 0L;
    private boolean blockHitPending = false;
    private long blockHitDelay = 0L;

    public ModuleAutoClicker() {
        super("AutoClicker", true, "Combat");
        addRangedConfig("CPSMax", 15, 0, 20);
        addRangedConfig("CPSMin", 10, 0, 20);
        addRangedConfig("BlockHitTicks", 1.5F, 1.0F, 20.0F);
        addConfig("WeaponsOnly", true);
        addConfig("AllowTools", false);
        addConfig("BreakBlocks", true);
        addConfig("BlockHit", true);
    }

    private long getNextClickDelay() {
        return 1000L / RandomUtil.nextLong(((Number) getConfig("CPSMin")).intValue(), ((Number) getConfig("CPSMax")).intValue());
    }

    private long getBlockHitDelay() {
        return (long) (50.0F * ((Number) getConfig("BlockHitTicks")).floatValue());
    }

    private boolean canClick() {
        if (!(Boolean) getConfig("WeaponsOnly")
                || ItemUtil.hasRawUnbreakingEnchant()
                || (Boolean) getConfig("AllowTools") && ItemUtil.isHoldingTool()) {
            if ((Boolean) getConfig("BreakBlocks") && this.isBreakingBlock()) {
                WorldSettings.GameType gameType12 = mc.playerController.getCurrentGameType();
                return gameType12 != WorldSettings.GameType.SURVIVAL && gameType12 != WorldSettings.GameType.CREATIVE;
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
        if (this.clickDelay > 0L) {
            this.clickDelay -= 50L;
        }
        if (mc.currentScreen != null) {
            this.clickPending = false;
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
                if (!mc.thePlayer.isUsingItem()) {
                    while (this.clickDelay <= 0L) {
                        this.clickPending = true;
                        this.clickDelay = this.clickDelay + this.getNextClickDelay();
                        KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                        KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindAttack.getKeyCode());
                    }
                }
                if ((Boolean) getConfig("BlockHit")
                        && this.blockHitDelay <= 0L
                        && mc.gameSettings.keyBindUseItem.isKeyDown()
                        && ItemUtil.isHoldingSword()) {
                    this.blockHitPending = true;
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                    if (!mc.thePlayer.isUsingItem()) {
                        this.blockHitDelay = this.blockHitDelay + this.getBlockHitDelay();
                        KeyBindUtil.pressKeyOnce(mc.gameSettings.keyBindUseItem.getKeyCode());
                    }
                }
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.clickDelay = 0L;
        this.blockHitDelay = 0L;
    }

    @Override
    public double getMaxValueForConfig(String key) {
        if (key == "CPSMin") return (Integer) getConfig("CPSMax");
        return super.getMaxValueForConfig(key);
    }

    @Override
    public double getMinValueForConfig(String key) {
        if (key == "CPSMax") return (Integer) getConfig("CPSMin");
        return super.getMinValueForConfig(key);
    }
}
