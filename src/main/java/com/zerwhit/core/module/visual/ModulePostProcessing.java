package com.zerwhit.core.module.visual;

import com.zerwhit.core.Meta;
import com.zerwhit.core.manager.ScreenEffects;
import com.zerwhit.core.module.IRenderModule;
import com.zerwhit.core.module.ModuleBase;

public class ModulePostProcessing extends ModuleBase implements IRenderModule {
    public ModulePostProcessing() {
        super("PostProcessing", false, "Visual");
        addConfig("Blur", true);
    }

    @Override
    public void onRender(float partialTicks, int screenWidth, int screenHeight) {
        Meta.blurEnabled = (Boolean) getConfig("Blur");
        ScreenEffects.INSTANCE.onRender();
    }
    
    @Override
    public void cycleStringConfig(String key) {
    }
}