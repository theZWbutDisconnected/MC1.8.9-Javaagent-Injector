package org.zerwhit.core.module;

public enum ToggleMode {
    TOGGLE,
    HOLD;
    
    public static ToggleMode fromString(String mode) {
        if (mode == null) {
            return TOGGLE;
        }
        
        try {
            return valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TOGGLE;
        }
    }
    
    public boolean isToggle() {
        return this == TOGGLE;
    }
    
    public boolean isHold() {
        return this == HOLD;
    }
}