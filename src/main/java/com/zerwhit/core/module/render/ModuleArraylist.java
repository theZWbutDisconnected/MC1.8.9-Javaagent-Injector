package com.zerwhit.core.module.render;

import com.zerwhit.core.Meta;
import com.zerwhit.core.module.IRenderModule;
import com.zerwhit.core.module.ModuleBase;
import com.zerwhit.core.Renderer;
import com.zerwhit.core.screen.ClickGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import java.util.ArrayList;
import java.util.List;

public class ModuleArraylist extends ModuleBase implements IRenderModule {
    public ModuleArraylist() {
        super("Arraylist", true, "Render");
        addConfig("BoxStyle", true);
    }

    @Override
    public void onEnable() {
        Meta.arraylistEnabled = true;
    }

    @Override
    public void onDisable() {
        Meta.arraylistEnabled = false;
    }

    @Override
    public void onRender(int screenWidth, int screenHeight) {
        if (!Meta.arraylistEnabled) return;
        
        List<ModuleBase> enabledModules = getEnabledModulesSorted();
        if (enabledModules.isEmpty()) return;
        boolean boxStyle = (Boolean) getConfig("BoxStyle");
        if (boxStyle) {
            renderBoxStyle(screenWidth, screenHeight, enabledModules);
        } else {
            renderNormalStyle(screenWidth, screenHeight, enabledModules);
        }
    }
    
    private void renderBoxStyle(int screenWidth, int screenHeight, List<ModuleBase> enabledModules) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        int x = screenWidth - 5;
        int y = 30;
        int moduleSpacing = 2;

        int maxWidth = 0;
        for (ModuleBase module : enabledModules) {
            int width = fontRenderer.getStringWidth(module.getDisplayName()) + 20;
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int backgroundWidth = maxWidth + 20;
        int totalHeight = (fontRenderer.FONT_HEIGHT + moduleSpacing) * (enabledModules.size() + 1) + 10;

        Renderer.drawRoundedRect(
                x - backgroundWidth,
                y,
                backgroundWidth,
                totalHeight,
                5,
                ClickGUI.colorScheme.withAlpha(ClickGUI.colorScheme.background, 0xD0)
        );

        String title = "ACTIVE MODULES";
        int titleWidth = fontRenderer.getStringWidth(title) + 20;
        Renderer.drawStringWithShadow(
                title,
                x - backgroundWidth + 10 + (backgroundWidth - titleWidth) / 2,
                y + 5,
                ClickGUI.colorScheme.accent
        );
        Renderer.drawRect(
                x - backgroundWidth + 5,
                y + fontRenderer.FONT_HEIGHT + 6,
                backgroundWidth - 10,
                1,
                ClickGUI.colorScheme.withAlpha(ClickGUI.colorScheme.primary, 0x80)
        );

        int moduleY = y + fontRenderer.FONT_HEIGHT + 10;
        for (ModuleBase module : enabledModules) {
            String moduleName = module.getDisplayName();
            int moduleNameWidth = fontRenderer.getStringWidth(moduleName);

            int moduleBgColor = ClickGUI.colorScheme.withAlpha(ClickGUI.colorScheme.moduleBackground, 0x60);
            Renderer.drawRoundedRect(
                    x - backgroundWidth + 5,
                    moduleY,
                    backgroundWidth - 10,
                    fontRenderer.FONT_HEIGHT,
                    3,
                    moduleBgColor
            );

            Renderer.drawStringWithShadow(
                    moduleName,
                    x - backgroundWidth + (backgroundWidth - moduleNameWidth) / 2,
                    moduleY + 1,
                    getModuleColor(module)
            );

            moduleY += fontRenderer.FONT_HEIGHT + moduleSpacing;
        }
    }
    
    private void renderNormalStyle(int screenWidth, int screenHeight, List<ModuleBase> enabledModules) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        int x = screenWidth - 5;
        int y = 30;
        int moduleSpacing = 2;

        int maxWidth = 0;
        for (ModuleBase module : enabledModules) {
            int width = fontRenderer.getStringWidth(module.getDisplayName());
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int moduleY = y;
        for (ModuleBase module : enabledModules) {
            String moduleName = module.getDisplayName();
            int moduleNameWidth = fontRenderer.getStringWidth(moduleName);
            
            Renderer.drawStringWithShadow(
                    moduleName,
                    x - moduleNameWidth,
                    moduleY,
                    getModuleColor(module)
            );

            moduleY += fontRenderer.FONT_HEIGHT + moduleSpacing;
        }
    }
    
    private List<ModuleBase> getEnabledModulesSorted() {
        List<ModuleBase> enabledModules = new ArrayList<>();
        for (List<ModuleBase> category : ModuleBase.categories.values()) {
            for (ModuleBase module : category) {
                if (module.enabled) {
                    enabledModules.add(module);
                }
            }
        }

        enabledModules.sort((m1, m2) -> {
            int categoryCompare = Integer.compare(m1.getCategoryOrder(), m2.getCategoryOrder());
            if (categoryCompare != 0) return categoryCompare;
            return m1.name.compareToIgnoreCase(m2.name);
        });

        return enabledModules;
    }
    
    private int getModuleColor(ModuleBase module) {
        switch(module.category) {
            case "Combat":
                return 0xFFFF6B6B;
            case "Movement":
                return 0xFF4ECDC4;
            case "Render":
                return 0xFF45B7D1;
            case "Visual":
                return 0xFFCD6DFF;
            default:
                return 0xFFFFFFFF;
        }
    }
}