package org.zerwhit.core.module.visual;

import org.zerwhit.core.data.Meta;
import org.zerwhit.core.manager.ScreenEffects;
import org.zerwhit.core.module.IRenderModule;
import org.zerwhit.core.module.ModuleBase;

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