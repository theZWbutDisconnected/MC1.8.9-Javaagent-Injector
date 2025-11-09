package com.zerwhit.core.module.combat;

import com.zerwhit.core.module.ITickableModule;
import com.zerwhit.core.module.ModuleBase;

public class ModuleReach extends ModuleBase implements ITickableModule {
    public ModuleReach() {
        super("Reach", true, "Combat");
        addConfig("Reach", 4.5);
    }
    
    public double getReach() {
        return enabled ? (Double) getConfig("Reach") : 3.0;
    }

    @Override
    public void onModuleTick() {

    }
    
    @Override
    public double getMaxValueForConfig(String key) {
        if ("Reach".equals(key)) {
            return 6.0;
        }
        return super.getMaxValueForConfig(key);
    }
}