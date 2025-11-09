package com.zerwhit.core.module.visual;

import com.zerwhit.core.module.IRenderModule;
import com.zerwhit.core.module.ModuleBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public class ModulePostProcessing extends ModuleBase implements IRenderModule {
    public ModulePostProcessing() {
        super("PostProcessing", false, "Visual");
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onRender(float partialTicks, int screenWidth, int screenHeight) {
        if (!enabled) return;
    }
    
    @Override
    public void cycleStringConfig(String key) {
    }
}