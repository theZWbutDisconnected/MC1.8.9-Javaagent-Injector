package org.zerwhit.core.module;

public interface IRenderModule {
    void onRender(float partialTicks, int screenWidth, int screenHeight);
    
    default boolean shouldRender() {
        return true;
    }
    
    default float getRenderPriority() {
        return 0.0f;
    }
    
    default boolean isOverlay() {
        return false;
    }
}