package org.zerwhit.core.util.island;

public class IslandNotification {
    public enum NotificationType {
        CUSTOM,
        MODULE_ENABLED,
        MODULE_DISABLED
    }
    
    private final NotificationType type;
    private final String title;
    private final String message;
    private final long timestamp;
    private final int duration;
    private final int priority;
    
    public IslandNotification(NotificationType type, String title, String message, int duration, int priority) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.duration = duration;
        this.priority = priority;
    }
    
    public IslandNotification(NotificationType type, String title, String message) {
        this(type, title, message, 3000, 1);
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > duration;
    }
    
    public float getProgress() {
        long elapsed = System.currentTimeMillis() - timestamp;
        return Math.min(1.0f, (float) elapsed / duration);
    }
}