package org.zerwhit.core.module;

public interface IEventModule {
    void onEvent(String eventType, Object... args);
    
    default boolean shouldHandleEvent(String eventType) {
        return true;
    }
    
    default int getEventPriority() {
        return 0;
    }
}