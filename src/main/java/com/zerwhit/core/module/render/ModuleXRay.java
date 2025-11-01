package com.zerwhit.core.module.render;

import com.zerwhit.core.module.Module;

import java.util.HashSet;
import java.util.Set;

public class ModuleXRay extends Module {
    private Set<Integer> xrayBlocks = new HashSet<>();
    
    public ModuleXRay() {
        super("X-Ray", false, "Render");
        addConfig("Opacity", 100);
        addConfig("Blocks", "56,14,15,16,21,129");
        
        String[] blocks = ((String) getConfig("Blocks")).split(",");
        for (String block : blocks) {
            xrayBlocks.add(Integer.parseInt(block.trim()));
        }
    }

    @Override
    public void onEnable() {
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void onDisable() {
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
    
    public boolean isXRayBlock(int blockId) {
        return xrayBlocks.contains(blockId);
    }
    
    public int getOpacity() {
        return (Integer) getConfig("Opacity");
    }
}