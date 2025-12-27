package org.zerwhit.core.manager;

import java.util.HashMap;
import java.util.Map;

public class AnimationManager {
    private final Map<String, AnimationState> animations = new HashMap<>();
    
    public static class AnimationState {
        private final long startTime;
        private final long duration;
        private final AnimationType type;
        private final float startValue;
        private final float endValue;
        
        public AnimationState(long duration, AnimationType type, float startValue, float endValue) {
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.type = type;
            this.startValue = startValue;
            this.endValue = endValue;
        }
        
        public float getCurrentValue() {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0f, (float) elapsed / duration);
            
            switch (type) {
                case EASE_OUT:
                    progress = easeOutCubic(progress);
                    break;
                case EASE_IN_OUT:
                    progress = easeInOutCubic(progress);
                    break;
                case LINEAR:
                default:
                    break;
            }
            
            return startValue + (endValue - startValue) * progress;
        }
        
        public boolean isFinished() {
            return System.currentTimeMillis() - startTime >= duration;
        }
        
        private float easeOutCubic(float x) {
            return (float) (1 - Math.pow(1 - x, 3));
        }
        
        private float easeInOutCubic(float x) {
            return (float) (x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2);
        }
    }
    
    public enum AnimationType {
        LINEAR,
        EASE_OUT,
        EASE_IN_OUT
    }
    
    public void startAnimation(String key, long duration, AnimationType type, float startValue, float endValue) {
        animations.put(key, new AnimationState(duration, type, startValue, endValue));
    }
    
    public float getAnimationValue(String key, float defaultValue) {
        AnimationState state = animations.get(key);
        if (state == null || state.isFinished()) {
            if (state != null && state.isFinished()) {
                animations.remove(key);
            }
            return defaultValue;
        }
        return state.getCurrentValue();
    }
    
    public boolean isAnimationActive(String key) {
        AnimationState state = animations.get(key);
        return state != null && !state.isFinished();
    }
    
    public void removeAnimation(String key) {
        animations.remove(key);
    }
    
    public void clearAnimations() {
        animations.clear();
    }
}