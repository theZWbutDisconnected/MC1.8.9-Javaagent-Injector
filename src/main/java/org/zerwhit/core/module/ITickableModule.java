package org.zerwhit.core.module;

public interface ITickableModule {
    void onModuleTick();
    
    default int getTickDelay() {
        return 1;
    }
    
    default boolean shouldTick() {
        return true;
    }
}