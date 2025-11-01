package com.zerwhit.core.screen;

import com.zerwhit.core.module.Module;
import com.zerwhit.core.ColorScheme;
import com.zerwhit.core.Meta;
import com.zerwhit.core.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zerwhit.core.Control.*;
import static com.zerwhit.core.module.Module.categories;

public class ClickGUI extends GuiScreen {
    public static final ColorScheme colorScheme = new ColorScheme();

    private static final int WINDOW_WIDTH = 400;
    private static final int WINDOW_HEIGHT = 300;
    private static final int BORDER_SIZE = 6;
    private static final int SIDEBAR_WIDTH = 100 - BORDER_SIZE;
    private static final int MODULE_LIST_WIDTH = WINDOW_WIDTH - BORDER_SIZE * 2 - SIDEBAR_WIDTH;

    private int windowX, windowY = 50;
    private boolean dragging = false;
    private int dragX, dragY;

    private String currentCategory = "Combat";

    private int sidebarScrollOffset = 0;
    private int sidebarTotalHeight = 0;
    private boolean isScrollingSidebar = false;

    private int moduleListScrollOffset = 0;
    private int moduleListTotalHeight = 0;
    private boolean isScrollingModuleList = false;

    private long animationStartTime = 0;
    private boolean isAnimating = false;
    private float currentScale = 0.0f;
    private float currentAlpha = 0.0f;
    private static final long ANIMATION_DURATION = 300;

    private Module selectedModule = null;
    private boolean showConfigMenu = false;
    private int configMenuX, configMenuY;
    private static final int CONFIG_MENU_WIDTH = 180;
    private static final int CONFIG_ITEM_HEIGHT = 25;

    private String editingConfigKey = null;
    private boolean isDraggingSlider = false;
    private Map<String, String> configInputValues = new HashMap<>();

    public static ClickGUI INSTANCE = new ClickGUI().init();

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!Meta.clickGUIOpened)
            return;

        updateAnimation();

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        windowX = Math.max(0, Math.min(windowX, screenWidth - WINDOW_WIDTH));
        windowY = Math.max(0, Math.min(windowY, screenHeight - WINDOW_HEIGHT));

        if (isAnimating) {
            GlStateManager.pushMatrix();
            float centerX = windowX + (float) WINDOW_WIDTH / 2;
            float centerY = windowY + (float) WINDOW_HEIGHT / 2;
            GlStateManager.translate(centerX, centerY, 0);
            GlStateManager.scale(currentScale, currentScale, 1.0f);
            GlStateManager.translate(-centerX, -centerY, 0);
        }
        drawGUIWithAlpha();

        if (showConfigMenu) {
            drawConfigMenu();
        }

        if (isAnimating) {
            GlStateManager.popMatrix();
        }
        handleDragging();
        handleScroll();
        handleSliderDrag();
    }

    private void drawGUIWithAlpha() {
        int originalPrimary = colorScheme.primary;
        int originalSecondary = colorScheme.secondary;
        int originalAccent = colorScheme.accent;
        int originalBackground = colorScheme.background;
        int originalText = colorScheme.text;
        int originalTextDisabled = colorScheme.textDisabled;
        int originalModuleBackground = colorScheme.moduleBackground;
        int originalModuleHover = colorScheme.moduleHover;

        if (isAnimating) {
            int alpha = (int) (currentAlpha * 255);
            colorScheme.primary = colorScheme.mulAlpha(originalPrimary, alpha);
            colorScheme.secondary = colorScheme.mulAlpha(originalSecondary, alpha);
            colorScheme.accent = colorScheme.mulAlpha(originalAccent, alpha);
            colorScheme.background = colorScheme.mulAlpha(originalBackground, alpha);
            colorScheme.text = colorScheme.mulAlpha(originalText, alpha);
            colorScheme.textDisabled = colorScheme.mulAlpha(originalTextDisabled, alpha);
            colorScheme.moduleBackground = colorScheme.mulAlpha(originalModuleBackground, alpha);
            colorScheme.moduleHover = colorScheme.mulAlpha(originalModuleHover, alpha);
        }

        Renderer.drawRoundedRect(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, BORDER_SIZE, colorScheme.background);

        Renderer.drawGradientRect(windowX, windowY, WINDOW_WIDTH, 25, colorScheme.primary, colorScheme.secondary);
        Renderer.drawRoundedRect(windowX, windowY, WINDOW_WIDTH, 25, BORDER_SIZE, colorScheme.primary);
        Renderer.drawStringWithShadow("ZeroClient v1.0", windowX + 10, windowY + 8, colorScheme.text);

        boolean closeHovered = isMouseOverCloseButton();
        Renderer.drawRect(windowX + WINDOW_WIDTH - 20, windowY + 5, 15, 15,
                closeHovered ? colorScheme.accent : colorScheme.text);
        Renderer.drawStringWithShadow("X", windowX + WINDOW_WIDTH - 15, windowY + 7, colorScheme.background);

        Renderer.drawRect(windowX + BORDER_SIZE, windowY + 25, SIDEBAR_WIDTH, WINDOW_HEIGHT - 25 - BORDER_SIZE,
                colorScheme.moduleBackground);
        Renderer.drawRect(windowX + SIDEBAR_WIDTH + BORDER_SIZE, windowY + 25, MODULE_LIST_WIDTH,
                WINDOW_HEIGHT - 25 - BORDER_SIZE, colorScheme.background);

        List<String> categoryList = new ArrayList<>(categories.keySet());
        int categoryHeight = 20;
        int categorySpacing = 2;
        sidebarTotalHeight = categoryList.size() * (categoryHeight + categorySpacing);

        int sidebarVisibleHeight = WINDOW_HEIGHT - 25 - BORDER_SIZE;
        if (sidebarTotalHeight > sidebarVisibleHeight) {
            Renderer.drawRect(windowX + BORDER_SIZE + SIDEBAR_WIDTH - 5, windowY + 25, 5, sidebarVisibleHeight,
                    colorScheme.moduleHover);
            int scrollbarHeight = (int) ((float) sidebarVisibleHeight / sidebarTotalHeight * sidebarVisibleHeight);
            int scrollbarY = windowY + 25
                    + (int) ((float) sidebarScrollOffset / sidebarTotalHeight * sidebarVisibleHeight);
            Renderer.drawRect(windowX + BORDER_SIZE + SIDEBAR_WIDTH - 5, scrollbarY, 5, scrollbarHeight,
                    colorScheme.accent);
        }

        int categoryY = windowY + 25 - sidebarScrollOffset;
        for (String category : categoryList) {
            if (categoryY + categoryHeight >= windowY + 25 && categoryY <= windowY + WINDOW_HEIGHT) {
                boolean isCurrent = category.equals(currentCategory);
                boolean hovered = isMouseOverCategory(category, windowX, categoryY);
                int color = isCurrent ? colorScheme.accent
                        : (hovered ? colorScheme.moduleHover : colorScheme.moduleBackground);
                Renderer.drawRect(windowX + BORDER_SIZE, categoryY, SIDEBAR_WIDTH, categoryHeight, color);
                Renderer.drawStringWithShadow(category, windowX + 5 + BORDER_SIZE, categoryY + 6, colorScheme.text);
            }
            categoryY += categoryHeight + categorySpacing;
        }

        List<Module> currentModules = categories.get(currentCategory);
        if (currentModules == null)
            currentModules = new ArrayList<>();
        int moduleHeight = 25;
        moduleListTotalHeight = currentModules.size() * moduleHeight;

        int moduleListVisibleHeight = WINDOW_HEIGHT - 25 - BORDER_SIZE;
        if (moduleListTotalHeight > moduleListVisibleHeight) {
            Renderer.drawRect(windowX + BORDER_SIZE + WINDOW_WIDTH - 5, windowY + 25, 5, moduleListVisibleHeight,
                    colorScheme.moduleHover);
            int scrollbarHeight = (int) ((float) moduleListVisibleHeight / moduleListTotalHeight
                    * moduleListVisibleHeight);
            int scrollbarY = windowY + 25
                    + (int) ((float) moduleListScrollOffset / moduleListTotalHeight * moduleListVisibleHeight);
            Renderer.drawRect(windowX + BORDER_SIZE + WINDOW_WIDTH - 5, scrollbarY, 5, scrollbarHeight,
                    colorScheme.accent);
        }

        int moduleY = windowY + 25 - moduleListScrollOffset;
        for (Module module : currentModules) {
            if (moduleY + moduleHeight >= windowY + 25 && moduleY <= windowY + WINDOW_HEIGHT - BORDER_SIZE) {
                renderModule(module, windowX + SIDEBAR_WIDTH + 10 + BORDER_SIZE, moduleY);
            }
            moduleY += moduleHeight;
        }

        if (isAnimating) {
            colorScheme.primary = originalPrimary;
            colorScheme.secondary = originalSecondary;
            colorScheme.accent = originalAccent;
            colorScheme.background = originalBackground;
            colorScheme.text = originalText;
            colorScheme.textDisabled = originalTextDisabled;
            colorScheme.moduleBackground = originalModuleBackground;
            colorScheme.moduleHover = originalModuleHover;
        }
    }

    private void updateAnimation() {
        if (isAnimating) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - animationStartTime;

            if (elapsed >= ANIMATION_DURATION) {
                currentScale = 1.0f;
                currentAlpha = 1.0f;
                isAnimating = false;
            } else {
                float progress = (float) elapsed / ANIMATION_DURATION;
                float easedProgress = easeOutCubic(progress);
                currentScale = 0.7f + 0.3f * easedProgress;
                currentAlpha = easedProgress;
            }
        }
    }

    private float easeOutCubic(float x) {
        return (float) (1 - Math.pow(1 - x, 3));
    }

    private void renderModule(Module module, int x, int y) {
        boolean hovered = isMouseOverModule(x, y);

        int bgColor = hovered ? colorScheme.moduleHover : colorScheme.moduleBackground;
        Renderer.drawRoundedRect(x, y, MODULE_LIST_WIDTH - 20, 20, 5, bgColor);

        Renderer.drawStringWithShadow(module.name, x + 5, y + 6,
                module.enabled ? colorScheme.accent : colorScheme.text);

        int toggleX = x + MODULE_LIST_WIDTH - 60;
        int toggleColor = module.enabled ? colorScheme.accent : colorScheme.textDisabled;
        Renderer.drawRoundedRect(toggleX, y + 5, 30, 10, 5, toggleColor);

        int sliderX = module.enabled ? toggleX + 20 : toggleX;
        Renderer.drawCircle(sliderX + 5, y + 10, 4, colorScheme.background);
    }

    private boolean isMouseOverCloseButton() {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= windowX + WINDOW_WIDTH - 20 &&
                mouseX <= windowX + WINDOW_WIDTH - 5 &&
                mouseY >= windowY + 5 &&
                mouseY <= windowY + 20;
    }

    private boolean isMouseOverCategory(String category, int x, int y) {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= x &&
                mouseX <= x + SIDEBAR_WIDTH &&
                mouseY >= y &&
                mouseY <= y + 20;
    }

    private boolean isMouseOverModule(int x, int y) {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= x &&
                mouseX <= x + MODULE_LIST_WIDTH - 20 &&
                mouseY >= y &&
                mouseY <= y + 20;
    }

    private boolean isMouseOverTitleBar() {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= windowX &&
                mouseX <= windowX + WINDOW_WIDTH &&
                mouseY >= windowY &&
                mouseY <= windowY + 25;
    }

    private boolean isMouseOverSidebarScrollBar() {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= windowX + SIDEBAR_WIDTH - 5 &&
                mouseX <= windowX + SIDEBAR_WIDTH &&
                mouseY >= windowY + 25 &&
                mouseY <= windowY + WINDOW_HEIGHT;
    }

    private boolean isMouseOverModuleListScrollBar() {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= windowX + WINDOW_WIDTH - 5 &&
                mouseX <= windowX + WINDOW_WIDTH &&
                mouseY >= windowY + 25 &&
                mouseY <= windowY + WINDOW_HEIGHT;
    }

    @Override
    public void onGuiClosed() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.currentScreen = null;
        mc.mouseHelper.grabMouseCursor();
        Meta.clickGUIOpened = false;
        resetAnimation();
    }

    public void handleMouseClick(int mouseX, int mouseY, int mouseButton) {
        if (!Meta.clickGUIOpened)
            return;

        if (mouseButton == 1) {
            List<Module> currentModules = categories.get(currentCategory);
            if (currentModules != null) {
                int moduleHeight = 25;
                int moduleY = windowY + 25 - moduleListScrollOffset;
                for (Module module : currentModules) {
                    if (moduleY + moduleHeight >= windowY + 25 && moduleY <= windowY + WINDOW_HEIGHT) {
                        if (isMouseOverModule(windowX + SIDEBAR_WIDTH + 10 + BORDER_SIZE, moduleY)) {
                            selectedModule = module;
                            showConfigMenu = true;
                            configMenuX = mouseX;
                            configMenuY = mouseY;
                            editingConfigKey = null;
                            isDraggingSlider = false;
                            return;
                        }
                    }
                    moduleY += moduleHeight;
                }
            }
            showConfigMenu = false;
            editingConfigKey = null;
            isDraggingSlider = false;
        } else if (mouseButton == 0) {
            if (showConfigMenu && isMouseOverConfigMenu(mouseX, mouseY)) {
                handleConfigMenuClick(mouseX, mouseY);
                return;
            }
            showConfigMenu = false;
            editingConfigKey = null;
            isDraggingSlider = false;

            if (isMouseOverCloseButton()) {
                ((GuiScreen) INSTANCE).onGuiClosed();
                return;
            }

            if (isMouseOverTitleBar()) {
                dragging = true;
                dragX = mouseX - windowX;
                dragY = mouseY - windowY;
                return;
            }

            List<String> categoryList = new ArrayList<>(categories.keySet());
            int categoryHeight = 20;
            int categorySpacing = 2;
            int categoryY = windowY + 25 - sidebarScrollOffset;
            for (String category : categoryList) {
                if (categoryY + categoryHeight >= windowY + 25 && categoryY <= windowY + WINDOW_HEIGHT) {
                    if (isMouseOverCategory(category, windowX, categoryY)) {
                        currentCategory = category;
                        moduleListScrollOffset = 0;
                        break;
                    }
                }
                categoryY += categoryHeight + categorySpacing;
            }

            List<Module> currentModules = categories.get(currentCategory);
            if (currentModules != null) {
                int moduleHeight = 25;
                int moduleY = windowY + 25 - moduleListScrollOffset;
                for (Module module : currentModules) {
                    if (moduleY + moduleHeight >= windowY + 25 && moduleY <= windowY + WINDOW_HEIGHT) {
                        if (isMouseOverModule(windowX + SIDEBAR_WIDTH + 10 + BORDER_SIZE, moduleY)) {
                            int toggleX = windowX + SIDEBAR_WIDTH + 10 + BORDER_SIZE + MODULE_LIST_WIDTH - 60;
                            if (mouseX >= toggleX && mouseX <= toggleX + 30 &&
                                    mouseY >= moduleY + 5 && mouseY <= moduleY + 15) {
                                module.enabled = !module.enabled;
                                if (module.enabled) {
                                    module.onEnable();
                                } else {
                                    module.onDisable();
                                }
                            }
                            break;
                        }
                    }
                    moduleY += moduleHeight;
                }
            }

            if (isMouseOverSidebarScrollBar()) {
                isScrollingSidebar = true;
                return;
            }

            if (isMouseOverModuleListScrollBar()) {
                isScrollingModuleList = true;
                return;
            }
        }
    }

    private void handleConfigMenuClick(int mouseX, int mouseY) {
        if (selectedModule == null)
            return;

        int itemY = configMenuY;
        for (Map.Entry<String, Object> entry : selectedModule.config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isMouseOverConfigItem(configMenuX, itemY, mouseX, mouseY)) {
                if (value instanceof Boolean) {
                    selectedModule.setConfig(key, !(Boolean) value);
                } else if (value instanceof Integer) {
                    editingConfigKey = key;
                    isDraggingSlider = true;
                    updateIntegerSliderValue(mouseX, key, (Integer) value);
                } else if (value instanceof Double) {
                    editingConfigKey = key;
                    isDraggingSlider = true;
                    updateDoubleSliderValue(mouseX, key, (Double) value);
                } else if (value instanceof String) {
                    selectedModule.cycleStringConfig(key);
                }
                break;
            }
            itemY += CONFIG_ITEM_HEIGHT;
        }
    }

    private void handleSliderDrag() {
        if (isDraggingSlider && editingConfigKey != null && selectedModule != null) {
            int mouseX = getMouseX();
            Object currentValue = selectedModule.getConfig(editingConfigKey);

            if (currentValue instanceof Integer) {
                updateIntegerSliderValue(mouseX, editingConfigKey, (Integer) currentValue);
            } else if (currentValue instanceof Double) {
                updateDoubleSliderValue(mouseX, editingConfigKey, (Double) currentValue);
            }
        }

        if (!Mouse.isButtonDown(0)) {
            isDraggingSlider = false;
            editingConfigKey = null;
        }
    }

    private void updateIntegerSliderValue(int mouseX, String key, int currentValue) {
        int sliderWidth = 80;
        int sliderX = configMenuX + CONFIG_MENU_WIDTH - sliderWidth - 5;

        float relativeX = Math.max(0, Math.min(mouseX - sliderX, sliderWidth));
        float percentage = relativeX / sliderWidth;

        int min = 0;
        int max = getMaxValueForConfig(key);
        int newValue = min + (int) (percentage * (max - min));

        selectedModule.setConfig(key, newValue);
    }

    private void updateDoubleSliderValue(int mouseX, String key, double currentValue) {
        int sliderWidth = 80;
        int sliderX = configMenuX + CONFIG_MENU_WIDTH - sliderWidth - 5;

        float relativeX = Math.max(0, Math.min(mouseX - sliderX, sliderWidth));
        float percentage = relativeX / sliderWidth;

        double min = 0.0;
        double max = getMaxDoubleValueForConfig(key);
        double newValue = min + percentage * (max - min);
        newValue = Math.round(newValue * 10.0) / 10.0;

        selectedModule.setConfig(key, newValue);
    }

    private void handleDragging() {
        if (dragging && Mouse.isButtonDown(0)) {
            int mouseX = getMouseX();
            int mouseY = getMouseY();
            windowX = mouseX - dragX;
            windowY = mouseY - dragY;
        } else {
            dragging = false;
        }
    }

    private void handleScroll() {
        int mouseWheel = Mouse.getDWheel();
        if (mouseWheel != 0) {
            if (isMouseOverSidebar()) {
                sidebarScrollOffset -= mouseWheel / 10;
                sidebarScrollOffset = Math.max(0,
                        Math.min(sidebarScrollOffset, sidebarTotalHeight - (WINDOW_HEIGHT - 25)));
            } else if (isMouseOverModuleList()) {
                moduleListScrollOffset -= mouseWheel / 10;
                moduleListScrollOffset = Math.max(0,
                        Math.min(moduleListScrollOffset, moduleListTotalHeight - (WINDOW_HEIGHT - 25)));
            }
        }

        if (isScrollingSidebar && Mouse.isButtonDown(0)) {
            int mouseY = getMouseY();
            float scrollPercentage = (float) (mouseY - windowY - 25) / (WINDOW_HEIGHT - 25);
            sidebarScrollOffset = (int) (scrollPercentage * sidebarTotalHeight);
            sidebarScrollOffset = Math.max(0, Math.min(sidebarScrollOffset, sidebarTotalHeight - (WINDOW_HEIGHT - 25)));
        } else {
            isScrollingSidebar = false;
        }

        if (isScrollingModuleList && Mouse.isButtonDown(0)) {
            int mouseY = getMouseY();
            float scrollPercentage = (float) (mouseY - windowY - 25) / (WINDOW_HEIGHT - 25);
            moduleListScrollOffset = (int) (scrollPercentage * moduleListTotalHeight);
            moduleListScrollOffset = Math.max(0,
                    Math.min(moduleListScrollOffset, moduleListTotalHeight - (WINDOW_HEIGHT - 25)));
        } else {
            isScrollingModuleList = false;
        }
    }

    private boolean isMouseOverSidebar() {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= windowX &&
                mouseX <= windowX + SIDEBAR_WIDTH &&
                mouseY >= windowY + 25 &&
                mouseY <= windowY + WINDOW_HEIGHT;
    }

    private boolean isMouseOverModuleList() {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= windowX + SIDEBAR_WIDTH &&
                mouseX <= windowX + WINDOW_WIDTH &&
                mouseY >= windowY + 25 &&
                mouseY <= windowY + WINDOW_HEIGHT;
    }


    private void drawConfigMenu() {
        if (!showConfigMenu || selectedModule == null)
            return;

        int x = configMenuX;
        int y = configMenuY;
        int height = selectedModule.config.size() * CONFIG_ITEM_HEIGHT;

        if (x + CONFIG_MENU_WIDTH > windowX + WINDOW_WIDTH) {
            x = windowX + WINDOW_WIDTH - CONFIG_MENU_WIDTH;
        }
        if (y + height > windowY + WINDOW_HEIGHT) {
            y = windowY + WINDOW_HEIGHT - height;
        }

        Renderer.drawRoundedRect(x, y, CONFIG_MENU_WIDTH, height, 5, colorScheme.background);

        int itemY = y;
        for (Map.Entry<String, Object> entry : selectedModule.config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            int mouseX = getMouseX();
            int mouseY = getMouseY();
            boolean hovered = isMouseOverConfigItem(x, itemY, mouseX, mouseY);
            int bgColor = hovered ? colorScheme.moduleHover : colorScheme.moduleBackground;

            Renderer.drawRect(x, itemY, CONFIG_MENU_WIDTH, CONFIG_ITEM_HEIGHT, bgColor);
            Renderer.drawStringWithShadow(key, x + 5, itemY + 6, colorScheme.text);

            if (value instanceof Boolean) {
                drawBooleanControl(x, itemY, key, (Boolean) value, mouseX, mouseY);
            } else if (value instanceof Integer) {
                drawIntegerControl(x, itemY, key, (Integer) value, mouseX, mouseY);
            } else if (value instanceof Double) {
                drawDoubleControl(x, itemY, key, (Double) value, mouseX, mouseY);
            } else if (value instanceof String) {
                drawStringControl(x, itemY, key, (String) value, mouseX, mouseY);
            } else {
                Renderer.drawStringWithShadow(String.valueOf(value), x + CONFIG_MENU_WIDTH - 50, itemY + 6, colorScheme.text);
            }

            itemY += CONFIG_ITEM_HEIGHT;
        }
    }

    private void drawBooleanControl(int x, int y, String key, boolean value, int mouseX, int mouseY) {
        int toggleWidth = 30;
        int toggleHeight = 15;
        int toggleX = x + CONFIG_MENU_WIDTH - toggleWidth - 5;
        int toggleY = y + (CONFIG_ITEM_HEIGHT - toggleHeight) / 2;

        boolean hovered = isMouseOverToggle(toggleX, toggleY, toggleWidth, toggleHeight, mouseX, mouseY);
        int toggleColor = value ? colorScheme.accent : colorScheme.textDisabled;

        if (hovered) {
            toggleColor = colorScheme.mulAlpha(toggleColor, 0.8f);
        }

        Renderer.drawRoundedRect(toggleX, toggleY, toggleWidth, toggleHeight, 7, toggleColor);

        int sliderX = value ? toggleX + toggleWidth - 10 : toggleX + 3;
        Renderer.drawCircle(sliderX + 4, toggleY + toggleHeight / 2, 4, colorScheme.background);

        String stateText = value ? "ON" : "OFF";
        Renderer.drawStringWithShadow(stateText, toggleX - 20, y + 6,
                value ? colorScheme.accent : colorScheme.textDisabled);
    }

    private void drawIntegerControl(int x, int y, String key, int value, int mouseX, int mouseY) {
        int sliderWidth = 80;
        int sliderHeight = 8;
        int sliderX = x + CONFIG_MENU_WIDTH - sliderWidth - 5;
        int sliderY = y + (CONFIG_ITEM_HEIGHT - sliderHeight) / 2;

        int min = 0;
        int max = getMaxValueForConfig(key);

        float percentage = (float) (value - min) / (max - min);
        int sliderPos = (int) (percentage * sliderWidth);

        Renderer.drawRect(sliderX, sliderY, sliderWidth, sliderHeight, colorScheme.moduleHover);
        Renderer.drawRect(sliderX, sliderY, sliderPos, sliderHeight, colorScheme.accent);

        int handleX = sliderX + sliderPos - 3;
        boolean draggingThis = isDraggingSlider && editingConfigKey != null && editingConfigKey.equals(key);
        boolean hovered = isMouseOverSliderHandle(handleX, sliderY, mouseX, mouseY) || draggingThis;

        int handleColor = hovered ? colorScheme.mulAlpha(colorScheme.accent, 0.8f) : colorScheme.accent;
        Renderer.drawCircle(handleX + 3, sliderY + sliderHeight / 2, 5, handleColor);

        String displayValue = String.valueOf(value);
        if (editingConfigKey != null && editingConfigKey.equals(key)) {
            displayValue = configInputValues.getOrDefault(key, displayValue);
        }
        Renderer.drawStringWithShadow(displayValue, sliderX - 25, y + 6, colorScheme.text);
    }

    private void drawDoubleControl(int x, int y, String key, double value, int mouseX, int mouseY) {
        int sliderWidth = 80;
        int sliderHeight = 8;
        int sliderX = x + CONFIG_MENU_WIDTH - sliderWidth - 5;
        int sliderY = y + (CONFIG_ITEM_HEIGHT - sliderHeight) / 2;

        double min = 0.0;
        double max = getMaxDoubleValueForConfig(key);

        float percentage = (float) ((value - min) / (max - min));
        int sliderPos = (int) (percentage * sliderWidth);

        Renderer.drawRect(sliderX, sliderY, sliderWidth, sliderHeight, colorScheme.moduleHover);
        Renderer.drawRect(sliderX, sliderY, sliderPos, sliderHeight, colorScheme.accent);

        int handleX = sliderX + sliderPos - 3;
        boolean draggingThis = isDraggingSlider && editingConfigKey != null && editingConfigKey.equals(key);
        boolean hovered = isMouseOverSliderHandle(handleX, sliderY, mouseX, mouseY) || draggingThis;

        int handleColor = hovered ? colorScheme.mulAlpha(colorScheme.accent, 0.8f) : colorScheme.accent;
        Renderer.drawCircle(handleX + 3, sliderY + sliderHeight / 2, 5, handleColor);

        String displayValue = String.format("%.1f", value);
        if (editingConfigKey != null && editingConfigKey.equals(key)) {
            displayValue = configInputValues.getOrDefault(key, displayValue);
        }
        Renderer.drawStringWithShadow(displayValue, sliderX - 30, y + 6, colorScheme.text);
    }

    private void drawStringControl(int x, int y, String key, String value, int mouseX, int mouseY) {
        int buttonWidth = 60;
        int buttonHeight = 16;
        int buttonX = x + CONFIG_MENU_WIDTH - buttonWidth - 5;
        int buttonY = y + (CONFIG_ITEM_HEIGHT - buttonHeight) / 2;

        boolean hovered = isMouseOverToggle(buttonX, buttonY, buttonWidth, buttonHeight, mouseX, mouseY);
        int buttonColor = hovered ? colorScheme.moduleHover : colorScheme.background;

        Renderer.drawRoundedRect(buttonX, buttonY, buttonWidth, buttonHeight, 3, buttonColor);
        Renderer.drawStringWithShadow(value, buttonX + 5, buttonY + 4, colorScheme.text);

        Renderer.drawStringWithShadow(value, buttonX - 40, y + 6, colorScheme.text);
    }

    private int getMaxValueForConfig(String key) {
        switch (key) {
            case "CPS": return 20;
            case "Delay": return 1000;
            case "Range": return 10;
            case "Opacity": return 100;
            case "Reach": return 10;
            default: return 100;
        }
    }

    private double getMaxDoubleValueForConfig(String key) {
        switch (key) {
            case "Speed": return 5.0;
            default: return 10.0;
        }
    }

    private boolean isMouseOverToggle(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isMouseOverSliderHandle(int handleX, int sliderY, int mouseX, int mouseY) {
        return mouseX >= handleX && mouseX <= handleX + 6 &&
                mouseY >= sliderY - 5 && mouseY <= sliderY + 13;
    }

    private boolean isMouseOverConfigMenu(int mouseX, int mouseY) {
        return mouseX >= configMenuX &&
                mouseX <= configMenuX + CONFIG_MENU_WIDTH &&
                mouseY >= configMenuY &&
                mouseY <= configMenuY + selectedModule.config.size() * CONFIG_ITEM_HEIGHT;
    }

    private boolean isMouseOverConfigItem(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x &&
                mouseX <= x + CONFIG_MENU_WIDTH &&
                mouseY >= y &&
                mouseY <= y + CONFIG_ITEM_HEIGHT;
    }

    public ClickGUI init() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        windowX = (scaledResolution.getScaledWidth() - WINDOW_WIDTH) / 2;
        windowY = (scaledResolution.getScaledHeight() - WINDOW_HEIGHT) / 2;
        return this;
    }

    public void playOpenAnimation() {
        animationStartTime = System.currentTimeMillis();
        isAnimating = true;
        currentScale = 0.7f;
        currentAlpha = 0.0f;
    }

    private void resetAnimation() {
        isAnimating = false;
        currentScale = 1.0f;
        currentAlpha = 1.0f;
    }
}