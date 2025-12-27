package org.zerwhit.core.module.visual;

import org.zerwhit.core.data.Meta;
import org.zerwhit.core.module.IRenderModule;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.module.ToggleMode;
import org.zerwhit.core.util.island.DynamicIslandRenderer;
import org.zerwhit.core.util.island.IslandNotification;

public class ModuleDynamicIsland extends ModuleBase implements IRenderModule, ITickableModule {
    public static ModuleDynamicIsland INSTANCE = new ModuleDynamicIsland();
    private DynamicIslandRenderer renderer;
    
    public ModuleDynamicIsland() {
        super("DynamicIsland", true, "Visual");
        addConfig("PositionX", 0.5);
        addConfig("PositionY", 0.1);
        addConfig("ShowChatMessages", true);
        addConfig("ShowPlayerEvents", true);
        addConfig("ShowGameEvents", true);
        addConfig("AnimationSpeed", 1.0);
        renderer = new DynamicIslandRenderer();
    }

    public static ModuleDynamicIsland getInstance() {
        return INSTANCE;
    }

    @Override
    public void onModuleTick() {
        renderer.update();
        
        if ((Boolean) getConfig("Enabled")) {
            Meta.dynamicIslandEnabled = true;
        } else {
            Meta.dynamicIslandEnabled = false;
        }
    }
    
    @Override
    public void onRender(float partialTicks, int screenWidth, int screenHeight) {
        renderer.render(partialTicks, screenWidth, screenHeight);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        Meta.dynamicIslandEnabled = true;
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        Meta.dynamicIslandEnabled = false;
        renderer.clearAllNotifications();
    }
    
    public void addCustomNotification(String title, String message, IslandNotification.NotificationType type, int duration, int priority) {
        IslandNotification notification = new IslandNotification(type, title, message, duration, priority);
        renderer.addNotification(notification);
    }

    public void addGameEventNotification(String title, String message) {
        if (!(Boolean) getConfig("ShowGameEvents")) {
            return;
        }
        
        IslandNotification notification = new IslandNotification(
            IslandNotification.NotificationType.CUSTOM,
            title,
            message,
            3000,
            1
        );
        renderer.addNotification(notification);
    }
    
    public void clearAllNotifications() {
        renderer.clearAllNotifications();
    }
    
    public boolean hasActiveNotifications() {
        return renderer.hasActiveNotifications();
    }
}