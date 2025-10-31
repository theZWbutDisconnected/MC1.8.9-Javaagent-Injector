package com.zerwhit.core.module;

import net.minecraft.client.Minecraft;

public class ModuleReach extends Module {
    public ModuleReach() {
        super("Reach", true, "Combat");
        addConfig("Reach", 4.5);
    }
    
    public double getReach() {
        return enabled ? (Double) getConfig("Reach") : 3.0;
    }
}