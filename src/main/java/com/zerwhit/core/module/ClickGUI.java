package com.zerwhit.core.module;

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

import static com.zerwhit.core.Control.getMouseX;
import static com.zerwhit.core.Control.getMouseY;

public class ClickGUI extends GuiScreen {
    private static final Map<String, List<Module>> categories = new HashMap<>();
    private static final ColorScheme colorScheme = new ColorScheme();

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

    public static ClickGUI INSTANCE = new ClickGUI().init();

    static {
        addModule(new Module("KillAura", true, "Combat"));
        addModule(new Module("Fly", false, "Movement"));
        addModule(new Module("Speed", true, "Movement"));
        addModule(new Module("NoFall", true, "Movement"));
        addModule(new Module("X-Ray", false, "Render"));
        addModule(new Module("AutoClicker", false, "Combat"));
        addModule(new Module("Reach", true, "Combat"));
    }

    private static void addModule(Module module) {
        categories.computeIfAbsent(module.category, k -> new ArrayList<>()).add(module);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!Meta.clickGUIOpened) return;

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
        if (isAnimating) {
            GlStateManager.popMatrix();
        }
        handleDragging();
        handleScroll();
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
            int alpha = (int)(currentAlpha * 255);
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
        Renderer.drawRect(windowX + WINDOW_WIDTH - 20, windowY + 5, 15, 15, closeHovered ? colorScheme.accent : colorScheme.text);
        Renderer.drawStringWithShadow("X", windowX + WINDOW_WIDTH - 15, windowY + 7, colorScheme.background);

        Renderer.drawRect(windowX + BORDER_SIZE, windowY + 25, SIDEBAR_WIDTH, WINDOW_HEIGHT - 25- BORDER_SIZE, colorScheme.moduleBackground);
        Renderer.drawRect(windowX + SIDEBAR_WIDTH + BORDER_SIZE, windowY + 25, MODULE_LIST_WIDTH, WINDOW_HEIGHT - 25 - BORDER_SIZE, colorScheme.background);

        List<String> categoryList = new ArrayList<>(categories.keySet());
        int categoryHeight = 20;
        int categorySpacing = 2;
        sidebarTotalHeight = categoryList.size() * (categoryHeight + categorySpacing);

        int sidebarVisibleHeight = WINDOW_HEIGHT - 25 - BORDER_SIZE;
        if (sidebarTotalHeight > sidebarVisibleHeight) {
            Renderer.drawRect(windowX + BORDER_SIZE + SIDEBAR_WIDTH - 5, windowY + 25, 5, sidebarVisibleHeight, colorScheme.moduleHover);
            int scrollbarHeight = (int) ((float) sidebarVisibleHeight / sidebarTotalHeight * sidebarVisibleHeight);
            int scrollbarY = windowY + 25 + (int) ((float) sidebarScrollOffset / sidebarTotalHeight * sidebarVisibleHeight);
            Renderer.drawRect(windowX + BORDER_SIZE + SIDEBAR_WIDTH - 5, scrollbarY, 5, scrollbarHeight, colorScheme.accent);
        }

        int categoryY = windowY + 25 - sidebarScrollOffset;
        for (String category : categoryList) {
            if (categoryY + categoryHeight >= windowY + 25 && categoryY <= windowY + WINDOW_HEIGHT) {
                boolean isCurrent = category.equals(currentCategory);
                boolean hovered = isMouseOverCategory(category, windowX, categoryY);
                int color = isCurrent ? colorScheme.accent : (hovered ? colorScheme.moduleHover : colorScheme.moduleBackground);
                Renderer.drawRect(windowX + BORDER_SIZE, categoryY, SIDEBAR_WIDTH, categoryHeight, color);
                Renderer.drawStringWithShadow(category, windowX + 5 + BORDER_SIZE, categoryY + 6, colorScheme.text);
            }
            categoryY += categoryHeight + categorySpacing;
        }

        List<Module> currentModules = categories.get(currentCategory);
        if (currentModules == null) currentModules = new ArrayList<>();
        int moduleHeight = 25;
        moduleListTotalHeight = currentModules.size() * moduleHeight;

        int moduleListVisibleHeight = WINDOW_HEIGHT - 25 - BORDER_SIZE;
        if (moduleListTotalHeight > moduleListVisibleHeight) {
            Renderer.drawRect(windowX + BORDER_SIZE + WINDOW_WIDTH - 5, windowY + 25, 5, moduleListVisibleHeight, colorScheme.moduleHover);
            int scrollbarHeight = (int) ((float) moduleListVisibleHeight / moduleListTotalHeight * moduleListVisibleHeight);
            int scrollbarY = windowY + 25 + (int) ((float) moduleListScrollOffset / moduleListTotalHeight * moduleListVisibleHeight);
            Renderer.drawRect(windowX + BORDER_SIZE + WINDOW_WIDTH - 5, scrollbarY, 5, scrollbarHeight, colorScheme.accent);
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

        Renderer.drawStringWithShadow(module.name, x + 5, y + 6, module.enabled ? colorScheme.accent : colorScheme.text);

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

    public void handleMouseClick(int mouseX, int mouseY) {
        if (!Meta.clickGUIOpened) return;

        if (isMouseOverCloseButton()) {
            ((GuiScreen)INSTANCE).onGuiClosed();
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
                sidebarScrollOffset = Math.max(0, Math.min(sidebarScrollOffset, sidebarTotalHeight - (WINDOW_HEIGHT - 25)));
            }
            else if (isMouseOverModuleList()) {
                moduleListScrollOffset -= mouseWheel / 10;
                moduleListScrollOffset = Math.max(0, Math.min(moduleListScrollOffset, moduleListTotalHeight - (WINDOW_HEIGHT - 25)));
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
            moduleListScrollOffset = Math.max(0, Math.min(moduleListScrollOffset, moduleListTotalHeight - (WINDOW_HEIGHT - 25)));
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