package org.zerwhit.core.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.BlockUtil;
import org.zerwhit.core.util.ObfuscationReflectionHelper;

public class ModuleFastPlace extends ModuleBase implements ITickableModule {
    public ModuleFastPlace() {
        super("FastPlace", true, "Movement");
    }

    @Override
    public void onModuleTick() {
        if (BlockUtil.isHoldingBlock() && BlockUtil.isSolid(((ItemBlock)mc.thePlayer.getHeldItem().getItem()).block))
            ObfuscationReflectionHelper.setObfuscatedFieldValue(Minecraft.class, new String[] {"rightClickDelayTimer", "field_71467_ac"}, mc, 0);
    }
}
