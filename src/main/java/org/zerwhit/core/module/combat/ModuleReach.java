package org.zerwhit.core.module.combat;

import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;

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