package org.zerwhit.core.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;

import org.zerwhit.core.data.Meta;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class ScreenEffects {
    private static final Logger logger = LogManager.getLogger(ScreenEffects.class);
    
    private boolean initialized = false;

    private int blurShader;

    private int uniformDiffuseSampler;
    private int uniformInSize;
    private int uniformBlurDir;
    private int uniformRadius;
    
    private Framebuffer blurFramebuffer;

    public static final ScreenEffects INSTANCE = new ScreenEffects();

    public void init() throws IOException {
        {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("uniformDiffuseSampler", "DiffuseSampler");
            paramMap.put("uniformInSize", "InSize");
            paramMap.put("uniformBlurDir", "BlurDir");
            paramMap.put("uniformRadius", "Radius");
            blurShader = createProgramShader("blur", paramMap);
        }
        Minecraft mc = Minecraft.getMinecraft();
        blurFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
        blurFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
    }

    private int createProgramShader(String name, Map<String, String> params) throws IOException {
        int vertShader = 0;
        int fragShader = 0;
        int shaderProgram;

        try {
            vertShader = loadShader("/assets/zerwhit/shaders/" + name + ".vsh", GL20.GL_VERTEX_SHADER);
            fragShader = loadShader("/assets/zerwhit/shaders/" + name + ".fsh", GL20.GL_FRAGMENT_SHADER);

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vertShader);
            GL20.glAttachShader(shaderProgram, fragShader);
            GL20.glLinkProgram(shaderProgram);

            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new RuntimeException("Shader link error: " +
                        GL20.glGetProgramInfoLog(shaderProgram, 1024));
            }

            for (Map.Entry<String, String> entry : params.entrySet()) {
                Field field = getClass().getDeclaredField(entry.getKey());
                field.setAccessible(true);
                field.set(this, GL20.glGetUniformLocation(shaderProgram, entry.getValue()));
            }
            initialized = true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (vertShader != 0) GL20.glDeleteShader(vertShader);
            if (fragShader != 0) GL20.glDeleteShader(fragShader);
        }
        return shaderProgram;
    }

    public void onRender() {
        Minecraft mc = Minecraft.getMinecraft();

        Blur : {
            if (!Meta.blurEnabled || !initialized) break Blur;
            
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            
            int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int prevFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            
            Map<Integer, Object> paramMap = new HashMap<>();
            paramMap.put(uniformDiffuseSampler, 0);
            paramMap.put(uniformBlurDir, new Vector2f(1.0f, 1.0f));
            paramMap.put(uniformInSize, new Vector2f(mc.displayWidth, mc.displayHeight));
            paramMap.put(uniformRadius, 8.0f);
            
            Framebuffer mainFramebuffer = mc.getFramebuffer();
            blurFramebuffer.bindFramebuffer(true);
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                logger.error("Framebuffer not complete: {}", GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
                break Blur;
            }
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainFramebuffer.framebufferTexture);
            addDisplay(blurShader, paramMap);

            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
            
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void addDisplay(int shader, Map<Integer, Object> params) {
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer fb = mc.getFramebuffer();

        int prevShader = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(shader);
        int prevActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, fb.framebufferTexture);

        for (Map.Entry<Integer, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof FloatBuffer) {
                GL20.glUniform1(entry.getKey(), (FloatBuffer) value);
            } else if (value instanceof IntBuffer) {
                GL20.glUniform1(entry.getKey(), (IntBuffer) value);
            } else if (value instanceof Integer) {
                GL20.glUniform1i(entry.getKey(), (Integer) value);
            } else if (value instanceof Float) {
                GL20.glUniform1f(entry.getKey(), (Float) value);
            } else if (value instanceof Vector2f) {
                Vector2f vec = (Vector2f) value;
                GL20.glUniform2f(entry.getKey(), vec.x, vec.y);
            }
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth();
        int h = sr.getScaledHeight();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, w, 0.0, h, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer worldRenderer = tess.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldRenderer.color(255, 255, 255, 255);
        worldRenderer.pos(0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex();
        worldRenderer.pos(w, 0.0, 0.0).tex(1.0, 0.0).endVertex();
        worldRenderer.pos(w, h, 0.0).tex(1.0, 1.0).endVertex();
        worldRenderer.pos(0.0, h, 0.0).tex(0.0, 1.0).endVertex();
        tess.draw();
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
        GL13.glActiveTexture(prevActiveTexture);
        GL20.glUseProgram(prevShader);
    }

    private int loadShader(String path, int type) throws IOException {
        InputStream in = getClass().getResourceAsStream(path);
        if (in == null) {
            in = getClass().getResourceAsStream("/assets/zerwhit/shaders/base." + (type == 35632 ? "fsh" : "vsh"));
            if (in == null) {
                throw new IOException("Shader not found: " + path);
            }
        }

        String code = IOUtils.toString(in, "UTF-8");
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, code);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 1024);
            GL20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile error: " + log);
        }
        return shader;
    }
}