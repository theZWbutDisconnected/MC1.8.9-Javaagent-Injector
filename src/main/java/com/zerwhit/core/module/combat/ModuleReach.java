package com.zerwhit.core.module.combat;

import com.zerwhit.core.module.Module;

public class ModuleReach extends Module {
    public ModuleReach() {
        super("Reach", true, "Combat");
        addConfig("Reach", 4.5);
    }
    
    public double getReach() {
        return enabled ? (Double) getConfig("Reach") : 3.0;
    }
}