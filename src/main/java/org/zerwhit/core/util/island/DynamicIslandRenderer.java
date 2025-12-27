package org.zerwhit.core.util.island;

import net.minecraft.client.renderer.GlStateManager;
import org.zerwhit.core.Renderer;
import org.zerwhit.core.manager.AnimationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.zerwhit.core.util.ColorScheme;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class DynamicIslandRenderer {
    private static final int ISLAND_HEIGHT = 40;
    private static final int ISLAND_CORNER_RADIUS = 20;
    private static final int PADDING = 10;
    private static final int MESSAGE_SPACING = 5;
    private static final int MAX_VISIBLE_NOTIFICATIONS = 3;
    private static final int MIN_WIDTH = 120;
    private static final int MAX_WIDTH = 300;
    
    private final ColorScheme colorScheme;
    private final AnimationManager animationManager;
    private final List<IslandNotification> activeNotifications;
    private final List<IslandNotification> visibleNotifications;
    private final Map<IslandNotification, Integer> notificationTargetPositions;
    private final Map<IslandNotification, Float> notificationCurrentPositions;
    private long lastNotificationTime;
    
    public DynamicIslandRenderer() {
        this.colorScheme = new ColorScheme();
        this.animationManager = new AnimationManager();
        this.activeNotifications = new ArrayList<>();
        this.visibleNotifications = new ArrayList<>();
        this.notificationTargetPositions = new HashMap<>();
        this.notificationCurrentPositions = new HashMap<>();
        this.lastNotificationTime = 0;
    }
    
    public void addNotification(IslandNotification notification) {
        activeNotifications.add(notification);
        showNextNotification();
    }
    
    public void update() {
        cleanupExpiredNotifications();
        List<IslandNotification> toRemove = new ArrayList<>();
        for (IslandNotification notification : visibleNotifications) {
            if (notification.isExpired()) {
                String fadeKey = "fade_" + notification.hashCode();
                if (!animationManager.isAnimationActive(fadeKey)) {
                    animationManager.startAnimation(fadeKey, 300, 
                        AnimationManager.AnimationType.EASE_OUT, 1f, 0f);
                } else {
                    float fadeProgress = animationManager.getAnimationValue(fadeKey, 0f);
                    if (fadeProgress <= 0.01f) {
                        toRemove.add(notification);
                    }
                }
            }
        }
        for (IslandNotification notification : toRemove) {
            visibleNotifications.remove(notification);
            notificationTargetPositions.remove(notification);
            notificationCurrentPositions.remove(notification);
            String fadeKey = "fade_" + notification.hashCode();
            animationManager.removeAnimation(fadeKey);
            updateNotificationPositions();
        }
        if (!activeNotifications.isEmpty() && visibleNotifications.size() < MAX_VISIBLE_NOTIFICATIONS) {
            showNextNotification();
        }
        updatePositionAnimations();
        updateCurrentPositions();
    }
    
    public void render(float partialTicks, int screenWidth, int screenHeight) {
        if (visibleNotifications.isEmpty()) return;
        
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int scaledWidth = scaledResolution.getScaledWidth();
        int scaledHeight = scaledResolution.getScaledHeight();
        
        int centerX = scaledWidth / 2;
        int startY = PADDING;
        
        int maxVisible = Math.min(visibleNotifications.size(), MAX_VISIBLE_NOTIFICATIONS);
        
        for (int i = 0; i < maxVisible; i++) {
            IslandNotification notification = visibleNotifications.get(i);
            String fadeKey = "fade_" + notification.hashCode();
            float fadeProgress = animationManager.getAnimationValue(fadeKey, 1f);
            if (fadeProgress <= 0f) continue;
            float y = notificationCurrentPositions.getOrDefault(notification, (float)(startY + i * (ISLAND_HEIGHT + MESSAGE_SPACING)));
            if ((int)y + ISLAND_HEIGHT > scaledHeight - PADDING) {
                break;
            }
            float scale = 0.8f + 0.2f * fadeProgress;
            float alpha = fadeProgress;
            int dynamicWidth = calculateDynamicWidth(notification);
            renderIslandBackground(centerX, (int)y, scale, alpha, dynamicWidth);
            renderNotificationContent(centerX, (int)y, scale, alpha, dynamicWidth, notification);
            renderProgressBar(centerX, (int)y, scale, alpha, dynamicWidth, notification);
        }
    }
    
    private void renderIslandBackground(int centerX, int y, float scale, float alpha, int dynamicWidth) {
        int width = (int) (dynamicWidth * scale);
        int height = (int) (ISLAND_HEIGHT * scale);
        int x = centerX - width / 2;
        
        int backgroundColor = applyAlpha(colorScheme.dynamicIslandBackground, alpha);
        int borderColor = applyAlpha(colorScheme.dynamicIslandBorder, alpha * 0.8f);
        
        Renderer.drawRoundedRect(x, y, width, height, ISLAND_CORNER_RADIUS, backgroundColor);
        Renderer.drawRoundedRect(x + 1, y + 1, width - 2, height - 2, ISLAND_CORNER_RADIUS - 1, borderColor);
    }
    
    private void renderNotificationContent(int centerX, int y, float scale, float alpha, int dynamicWidth, IslandNotification notification) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        int width = (int) (dynamicWidth * scale);
        int height = (int) (ISLAND_HEIGHT * scale);
        int x = centerX - width / 2;
        
        int textColor = applyAlpha(colorScheme.dynamicIslandText, alpha);
        
        String title = notification.getTitle();
        String message = notification.getMessage();
        
        int titleX = x + (int)(PADDING * scale);
        int titleY = y + (int)(PADDING / 2 * scale);
        int messageX = titleX;
        int messageY = titleY + (int)(fontRenderer.FONT_HEIGHT * scale) + 2;
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(titleX, titleY, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        fontRenderer.drawStringWithShadow(title, 0, 0, textColor);
        GlStateManager.popMatrix();
        
        int maxMessageWidth = (int)((width - PADDING * 3 - 16) / scale);
        message = truncateMessageToWidth(fontRenderer, message, maxMessageWidth);
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(messageX, messageY, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        fontRenderer.drawStringWithShadow(message, 0, 0, textColor);
        GlStateManager.popMatrix();
        
        renderNotificationIcon(x + width - (int)(PADDING * scale) - 16, y + (int)(PADDING / 2 * scale), scale, alpha, notification);
    }
    
    private void renderNotificationIcon(int x, int y, float scale, float alpha, IslandNotification notification) {
        int iconColor = applyAlpha(colorScheme.dynamicIslandAccent, alpha);
        int size = 16;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0f);
        GlStateManager.scale(scale, scale, 1.0f);
        
        switch (notification.getType()) {
            case MODULE_DISABLED:
                GlStateManager.translate(0, 8 / scale, 0.0f);
                GlStateManager.scale(0.35f, 0.35f, 0.35f);
                Renderer.drawTexture("off", 0, 0);
                break;
            case MODULE_ENABLED:
                GlStateManager.translate(0, 8 / scale, 0.0f);
                GlStateManager.scale(0.35f, 0.35f, 0.35f);
                Renderer.drawTexture("on", 0, 0);
                break;
            default:
                GlStateManager.scale(0.2f, 0.2f, 0.2f);
                Renderer.drawTexture("clientlogonotitle", 0, 0);
                break;
        }
        GlStateManager.popMatrix();
    }
    
    private void renderProgressBar(int centerX, int y, float scale, float alpha, int dynamicWidth, IslandNotification notification) {
        int width = (int) (dynamicWidth - 60 * scale);
        int height = (int) (ISLAND_HEIGHT * scale);
        int x = centerX - width / 2;
        
        float progress = notification.getProgress();
        int progressWidth = (int) (width * progress);
        int progressColor = applyAlpha(colorScheme.dynamicIslandAccent, alpha * 0.6f);
        
        Renderer.drawRect(x, y + height - 10, progressWidth, 2, progressColor);
    }
    
    private void showNextNotification() {
        if (activeNotifications.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotificationTime < 200) {
            return;
        }
        
        IslandNotification nextNotification = getHighestPriorityNotification();
        if (nextNotification != null) {
            int insertIndex = insertNotificationInOrder(nextNotification);
            activeNotifications.remove(nextNotification);
            lastNotificationTime = currentTime;
            String fadeKey = "fade_" + nextNotification.hashCode();
            animationManager.startAnimation(fadeKey, 300, 
                AnimationManager.AnimationType.EASE_OUT, 0f, 1f);
            updateNotificationPositions();
        }
    }
    
    private int insertNotificationInOrder(IslandNotification notification) {
        for (int i = 0; i < visibleNotifications.size(); i++) {
            IslandNotification existing = visibleNotifications.get(i);
            if (notification.getPriority() > existing.getPriority()) {
                visibleNotifications.add(i, notification);
                return i;
            } else if (notification.getPriority() == existing.getPriority()) {
                if (notification.getTimestamp() > existing.getTimestamp()) {
                    visibleNotifications.add(i, notification);
                    return i;
                }
            }
        }
        visibleNotifications.add(notification);
        return visibleNotifications.size() - 1;
    }
    
    private void updateNotificationPositions() {
        for (int i = 0; i < visibleNotifications.size(); i++) {
            IslandNotification notification = visibleNotifications.get(i);
            int targetPosition = i;
            Integer currentTarget = notificationTargetPositions.get(notification);
            if (currentTarget == null || currentTarget != targetPosition) {
                notificationTargetPositions.put(notification, targetPosition);
            }
        }
    }
    
    private void updatePositionAnimations() {
        for (IslandNotification notification : visibleNotifications) {
            String positionKey = "position_" + notification.hashCode();
            if (animationManager.isAnimationActive(positionKey)) {
                animationManager.removeAnimation(positionKey);
            }
        }
    }
    
    private void updateCurrentPositions() {
        for (int i = 0; i < visibleNotifications.size(); i++) {
            IslandNotification notification = visibleNotifications.get(i);
            int targetY = PADDING + i * (ISLAND_HEIGHT + MESSAGE_SPACING);
            float currentY = notificationCurrentPositions.getOrDefault(notification, (float)targetY);
            float newY = currentY + (targetY - currentY) * 0.2f;
            notificationCurrentPositions.put(notification, newY);
        }
    }
    
    private IslandNotification getHighestPriorityNotification() {
        IslandNotification highestPriority = null;
        for (IslandNotification notification : activeNotifications) {
            if (highestPriority == null || notification.getPriority() > highestPriority.getPriority()) {
                highestPriority = notification;
            }
        }
        return highestPriority;
    }
    
    private void cleanupExpiredNotifications() {
        activeNotifications.removeIf(IslandNotification::isExpired);
    }
    
    private int applyAlpha(int color, float alpha) {
        int a = (int) (alpha * 255);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    private int calculateDynamicWidth(IslandNotification notification) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        String title = notification.getTitle();
        String message = notification.getMessage();
        
        int titleWidth = fontRenderer.getStringWidth(title);
        int messageWidth = fontRenderer.getStringWidth(message);
        
        int requiredWidth = Math.max(titleWidth, messageWidth) + PADDING * 3 + 16;
        
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, requiredWidth));
    }
    
    private String truncateMessageToWidth(FontRenderer fontRenderer, String message, int maxWidth) {
        if (fontRenderer.getStringWidth(message) <= maxWidth) {
            return message;
        }
        
        String ellipsis = "...";
        int ellipsisWidth = fontRenderer.getStringWidth(ellipsis);
        
        for (int i = message.length() - 1; i > 0; i--) {
            String truncated = message.substring(0, i) + ellipsis;
            if (fontRenderer.getStringWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }
        
        return ellipsis;
    }
    
    public void clearAllNotifications() {
        activeNotifications.clear();
        visibleNotifications.clear();
        animationManager.clearAnimations();
    }
    
    public boolean hasActiveNotifications() {
        return !visibleNotifications.isEmpty() || !activeNotifications.isEmpty();
    }
}