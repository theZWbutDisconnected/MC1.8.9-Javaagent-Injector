package com.zerwhit.core.module;

import com.zerwhit.core.ColorScheme;
import com.zerwhit.core.Meta;
import com.zerwhit.core.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

import static com.zerwhit.core.Control.getMouseX;
import static com.zerwhit.core.Control.getMouseY;

public class ClickGUI extends GuiScreen {
    private static final List<Module> modules = new ArrayList<>();
    private static final ColorScheme colorScheme = new ColorScheme();

    private static final int WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 300;

    private int windowX, windowY = 50;
    private boolean dragging = false;
    private int dragX, dragY;

    public static ClickGUI INSTANCE = new ClickGUI().init();

    static {
        modules.add(new Module("KillAura", true));
        modules.add(new Module("Fly", false));
        modules.add(new Module("Speed", true));
        modules.add(new Module("NoFall", true));
        modules.add(new Module("X-Ray", false));
        modules.add(new Module("AutoClicker", false));
        modules.add(new Module("Reach", true));
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!Meta.clickGUIOpened) return;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int screenWidth = scaledResolution.getScaledWidth();
        int screenHeight = scaledResolution.getScaledHeight();

        windowX = Math.max(0, Math.min(windowX, screenWidth - WINDOW_WIDTH));
        windowY = Math.max(0, Math.min(windowY, screenHeight - WINDOW_HEIGHT));
        Renderer.drawRoundedRect(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 8,
                colorScheme.background);

        Renderer.drawGradientRect(windowX, windowY, WINDOW_WIDTH, 25,
                colorScheme.primary,
                colorScheme.secondary);

        Renderer.drawRoundedRect(windowX, windowY, WINDOW_WIDTH, 25, 8,
                colorScheme.primary);

        Renderer.drawStringWithShadow("ZeroClient v1.0", windowX + 10, windowY + 8,
                colorScheme.text);

        boolean closeHovered = isMouseOverCloseButton();
        Renderer.drawRect(windowX + WINDOW_WIDTH - 20, windowY + 5, 15, 15,
                closeHovered ? colorScheme.accent : colorScheme.text);
        Renderer.drawStringWithShadow("X", windowX + WINDOW_WIDTH - 15, windowY + 7,
                colorScheme.background);

        int moduleY = windowY + 35;
        for (Module module : modules) {
            renderModule(module, windowX + 10, moduleY);
            moduleY += 25;
        }

        handleDragging();
    }

    private void renderModule(Module module, int x, int y) {
        boolean hovered = isMouseOverModule(x, y);

        int bgColor = hovered ? colorScheme.moduleHover : colorScheme.moduleBackground;
        Renderer.drawRoundedRect(x, y, WINDOW_WIDTH - 20, 20, 5, bgColor);

        Renderer.drawStringWithShadow(module.name, x + 5, y + 6,
                module.enabled ? colorScheme.accent : colorScheme.text);

        int toggleX = x + WINDOW_WIDTH - 60;
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

    private boolean isMouseOverModule(int x, int y) {
        int mouseX = getMouseX();
        int mouseY = getMouseY();

        return mouseX >= x &&
                mouseX <= x + WINDOW_WIDTH - 20 &&
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

    @Override
    public void onGuiClosed() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.currentScreen = null;
        mc.mouseHelper.grabMouseCursor();
        Meta.clickGUIOpened = false;
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

        int moduleY = windowY + 35;
        for (Module module : modules) {
            if (isMouseOverModule(windowX + 10, moduleY)) {
                int toggleX = windowX + 10 + WINDOW_WIDTH - 60;
                if (mouseX >= toggleX && mouseX <= toggleX + 30 &&
                        mouseY >= moduleY + 5 && mouseY <= moduleY + 15) {
                    module.enabled = !module.enabled;
                }
                break;
            }
            moduleY += 25;
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

    public ClickGUI init() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        windowX = (scaledResolution.getScaledWidth() - WINDOW_WIDTH) / 2;
        windowY = (scaledResolution.getScaledHeight() - WINDOW_HEIGHT) / 2;
        return this;
    }
}