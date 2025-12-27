package org.zerwhit.core.module;

public interface IVisualModule {
    void onHook(String funcName, float partialTicks);
    
    default boolean shouldRender() {
        return true;
    }
    
    default float getRenderPriority() {
        return 0.0f;
    }
}