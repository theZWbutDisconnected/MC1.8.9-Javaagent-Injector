package org.zerwhit.core;

import org.zerwhit.core.config.ConfigInitialization;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.manager.TextureLoader;
import org.zerwhit.core.manager.TextureRegistry;
import org.zerwhit.core.manager.ModuleManager;
import org.zerwhit.core.manager.RotationManager;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.resource.TextureResource;

import org.zerwhit.core.util.ObfuscationReflectionHelper;
import org.zerwhit.core.util.SafeLogger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Timer;
import net.minecraft.util.Vec3;

import java.util.concurrent.Callable;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import static org.zerwhit.core.util.ObfuscationReflectionHelper.*;

public class Hooks {
    private static final SafeLogger logger = SafeLogger.getLogger(Hooks.class);

    public static final ModuleManager moduleMng = new ModuleManager();
    public static final RotationManager rotMng = new RotationManager();
    
    private static boolean texturesInitialized = false;
    private static boolean modulesInitialized = false;
    private static final int MARGIN = 10;
    private static boolean slient;

    public static void onUpdateDisplay() {
        Minecraft mc = Minecraft.getMinecraft();
        if (rotMng.rendererViewEntity == null) {
            rotMng.rendererViewEntity = new Entity(Minecraft.getMinecraft().theWorld){
                @Override
                protected void entityInit() {

                }

                @Override
                protected void readEntityFromNBT(NBTTagCompound nbtTagCompound) {

                }

                @Override
                protected void writeEntityToNBT(NBTTagCompound nbtTagCompound) {

                }
            };
        }
        if (!modulesInitialized) {
            ConfigInitialization.initialize();
            moduleMng.initialize();
            modulesInitialized = true;
        }
        moduleMng.invokeHook(ModuleManager.ModuleHookType.TICK, "onUpdateDisplay");
        try {
            if (!texturesInitialized)
                initializeTextures();
            render(mc.displayWidth, mc.displayHeight);
        } catch (Exception e) {
            logger.error("Failed to render display: {}", e.getMessage());
            logger.error("Error details:", e);
        }
    }

    public static void onGameLoop() {
        Control.checkRShiftKey();
        ModuleBase.updateKeyBindings();
    }

    public static void onPreTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (mc.thePlayer.isUsingItem())
        {
            while (mc.gameSettings.keyBindAttack.isPressed())
            {
                ObfuscationReflectionHelper.invokeObfuscatedMethod(Minecraft.class, new String[]{"clickMouse", "func_147116_af"}, mc);
            }

            while (mc.gameSettings.keyBindUseItem.isPressed())
            {
                ObfuscationReflectionHelper.invokeObfuscatedMethod(Minecraft.class, new String[]{"rightClickMouse", "func_147121_ag"}, mc);
            }

            while (mc.gameSettings.keyBindPickBlock.isPressed())
            {
                ObfuscationReflectionHelper.invokeObfuscatedMethod(Minecraft.class, new String[]{"middleClickMouse", "func_147112_ai"}, mc);
            }
        }
    }
    public static void onPostTick() {}
    public static void onPlayerPreUpdate() {
        Meta.slientAimEnabled = false;
        moduleMng.invokeCategory(ModuleManager.ModuleCategory.COMBAT, ModuleManager.ModuleHookType.TICK);
        moduleMng.invokeCategory(ModuleManager.ModuleCategory.MOVEMENT, ModuleManager.ModuleHookType.TICK);
    }
    public static void onPlayerPostUpdate() {
        moduleMng.invokeCategory(ModuleManager.ModuleCategory.VISUAL, ModuleManager.ModuleHookType.TICK);
    }

    public static void onPlayerHurt() {
        moduleMng.invokeModule(ModuleManager.ModuleHookType.EVENT, "playerHurt");
    }
    
    /**
     * Hook method for intercepting EntityLivingBase's moveEntityWithHeading method
     * This method is called at the beginning of the original moveEntityWithHeading method
     * Args: strafe - the strafe movement input, forward - the forward movement input
     */
    public static void onMoveEntityWithHeading(float strafe, float forward) {
        moduleMng.invokeHook(ModuleManager.ModuleHookType.EVENT, "moveEntityWithHeading", strafe, forward);
    }

    private static void initializeTextures() {
        TextureRegistry.initialize();
        TextureLoader.loadAllTextureResources();
        texturesInitialized = true;
        logger.info("Texture system and ScreenEffects initialized");
    }

    private static void render(int screenWidth, int screenHeight) {
        drawVapeIcons(screenWidth);
        int scaledWidth = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
        int scaledHeight = new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();
        Timer timer = (Timer) ObfuscationReflectionHelper.getObfuscatedFieldValue(Minecraft.class, new String[]{"timer", "field_71428_T"}, Minecraft.getMinecraft());
        float partialTicks = timer != null ? timer.renderPartialTicks : 0;
        moduleMng.invokeHook(ModuleManager.ModuleHookType.RENDER, "onRenderDisplay", partialTicks, scaledWidth, scaledHeight);
    }

    private static void drawVapeIcons(int screenWidth) {
        TextureResource v4logoResource = TextureRegistry.getTextureResource("v4logo");
        TextureResource vapelogoResource = TextureRegistry.getTextureResource("clientlogo");

        Renderer.drawTexture("clientlogo", 20, MARGIN);
    }


    public static TextureResource getTextureResource(String key) {
        return TextureRegistry.getTextureResource(key);
    }

    public static boolean isTextureLoaded(String key) {
        return TextureRegistry.isTextureLoaded(key);
    }

    public static void cleanup() {
        
        ConfigInitialization.shutdown();
        
        TextureRegistry.cleanup();
        moduleMng.cleanup();
        texturesInitialized = false;
        modulesInitialized = false;
        logger.info("Hooks system cleaned up");
    }

    /**
     * Hook method for intercepting ItemRenderer's renderItemInFirstPerson method
     * This method is called at the beginning of the original renderItemInFirstPerson method
     * Args: partialTickTime - the partial tick time for interpolation
     */
    public static void renderItemInFirstPersonHook(float partialTicks) {
        moduleMng.invokeHook(ModuleManager.ModuleHookType.VISUAL, "renderItemInFirstPerson", partialTicks);
    }

    /**
     * Hook method for intercepting EntityRenderer's orientCamera method
     * This method is called at the beginning of the original orientCamera method
     * Args: partialTickTime - the partial tick time for interpolation
     */
    public static void orientCameraHook(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (rotMng.rendererViewEntity == null) {
            rotMng.rendererViewEntity = new Entity(Minecraft.getMinecraft().theWorld){
                @Override
                protected void entityInit() {

                }

                @Override
                protected void readEntityFromNBT(NBTTagCompound nbtTagCompound) {

                }

                @Override
                protected void writeEntityToNBT(NBTTagCompound nbtTagCompound) {

                }
            };
        }
        Entity entity = Meta.slientAimEnabled ? rotMng.rendererViewEntity : mc.getRenderViewEntity();
        entity.posX = mc.thePlayer.posX;
        entity.posY = mc.thePlayer.posY;
        entity.posZ = mc.thePlayer.posZ;
        entity.prevPosX = mc.thePlayer.prevPosX;
        entity.prevPosY = mc.thePlayer.prevPosY;
        entity.prevPosZ = mc.thePlayer.prevPosZ;
        EntityRenderer entityRenderer = Minecraft.getMinecraft().entityRenderer;
        float f = entity.getEyeHeight();
        double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks;
        double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks + (double)f;
        double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks;

        if (entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPlayerSleeping())
        {
            f = (float)((double)f + 1.0D);
            GlStateManager.translate(0.0F, 0.3F, 0.0F);

            if (!Minecraft.getMinecraft().gameSettings.debugCamEnable)
            {
                BlockPos blockpos = new BlockPos(entity);
                IBlockState iblockstate = Minecraft.getMinecraft().theWorld.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                if (block == Blocks.bed)
                {
                    int j = ((EnumFacing)iblockstate.getValue(BlockBed.FACING)).getHorizontalIndex();
                    GlStateManager.rotate((float)(j * 90), 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
            }
        }
        else if (Minecraft.getMinecraft().gameSettings.thirdPersonView > 0)
        {
            double d3 = (double)((float)getObfuscatedFieldValue(EntityRenderer.class, new String[]{"thirdPersonDistanceTemp", "field_78491_C"}, entityRenderer) + ((float)getObfuscatedFieldValue(EntityRenderer.class, new String[]{"thirdPersonDistance", "field_78490_B"}, entityRenderer) - (float)getObfuscatedFieldValue(EntityRenderer.class, new String[]{"thirdPersonDistanceTemp", "field_78491_C"}, entityRenderer)) * partialTicks);

            if (Minecraft.getMinecraft().gameSettings.debugCamEnable)
            {
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
            }
            else
            {
                float f1 = entity.rotationYaw;
                float f2 = entity.rotationPitch;

                if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2)
                {
                    f2 += 180.0F;
                }

                double d4 = (double)(-MathHelper.sin(f1 / 180.0F * (float)Math.PI) * MathHelper.cos(f2 / 180.0F * (float)Math.PI)) * d3;
                double d5 = (double)(MathHelper.cos(f1 / 180.0F * (float)Math.PI) * MathHelper.cos(f2 / 180.0F * (float)Math.PI)) * d3;
                double d6 = (double)(-MathHelper.sin(f2 / 180.0F * (float)Math.PI)) * d3;

                for (int i = 0; i < 8; ++i)
                {
                    float f3 = (float)((i & 1) * 2 - 1);
                    float f4 = (float)((i >> 1 & 1) * 2 - 1);
                    float f5 = (float)((i >> 2 & 1) * 2 - 1);
                    f3 = f3 * 0.1F;
                    f4 = f4 * 0.1F;
                    f5 = f5 * 0.1F;
                    MovingObjectPosition movingobjectposition = Minecraft.getMinecraft().theWorld.rayTraceBlocks(new Vec3(d0 + (double)f3, d1 + (double)f4, d2 + (double)f5), new Vec3(d0 - d4 + (double)f3 + (double)f5, d1 - d6 + (double)f4, d2 - d5 + (double)f5));

                    if (movingobjectposition != null)
                    {
                        double d7 = movingobjectposition.hitVec.distanceTo(new Vec3(d0, d1, d2));

                        if (d7 < d3)
                        {
                            d3 = d7;
                        }
                    }
                }

                if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2)
                {
                    GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, (float)(-d3));
                GlStateManager.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            }
        }
        else
        {
            GlStateManager.translate(0.0F, 0.0F, -0.1F);
        }

        if (!Minecraft.getMinecraft().gameSettings.debugCamEnable)
        {
            GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);

            if (entity instanceof EntityAnimal)
            {
                EntityAnimal entityanimal = (EntityAnimal)entity;
                GlStateManager.rotate(entityanimal.prevRotationYawHead + (entityanimal.rotationYawHead - entityanimal.prevRotationYawHead) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
            }
            else
            {
                GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, 1.0F, 0.0F);
            }
        }

        GlStateManager.translate(0.0F, -f, 0.0F);
        d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double)partialTicks;
        d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double)partialTicks + (double)f;
        d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double)partialTicks;
        setObfuscatedFieldValue(EntityRenderer.class, new String[]{"cloudFog", "field_78500_U"}, entityRenderer, Minecraft.getMinecraft().renderGlobal.hasCloudFog(d0, d1, d2, partialTicks));
    }

    public static void updateCameraAndRenderHook(float partialTicks, long nanoTime) {
        boolean flag = Display.isActive();

        EntityRenderer entityRenderer = Minecraft.getMinecraft().entityRenderer;
        if (!flag && Minecraft.getMinecraft().gameSettings.pauseOnLostFocus && (!Minecraft.getMinecraft().gameSettings.touchscreen || !Mouse.isButtonDown(1)))
        {
            long prevFrameTime = (Long) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"prevFrameTime", "field_78508_Y"}, entityRenderer);
            if (Minecraft.getSystemTime() - prevFrameTime > 500L)
            {
                Minecraft.getMinecraft().displayInGameMenu();
            }
        }
        else
        {
            setObfuscatedFieldValue(EntityRenderer.class, new String[]{"prevFrameTime", "field_78508_Y"}, entityRenderer, Minecraft.getSystemTime());
        }

        Minecraft.getMinecraft().mcProfiler.startSection("mouse");

        if (flag && Minecraft.isRunningOnMac && (Boolean) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"inGameHasFocus", "field_71415_G"}, entityRenderer) && !Mouse.isInsideWindow())
        {
            Mouse.setGrabbed(false);
            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
            Mouse.setGrabbed(true);
        }

        if (Minecraft.getMinecraft().inGameHasFocus && flag)
        {
            Minecraft.getMinecraft().mouseHelper.mouseXYChange();
            float f = Minecraft.getMinecraft().gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f1 = f * f * f * 8.0F;
            float f2 = (float)Minecraft.getMinecraft().mouseHelper.deltaX * f1;
            float f3 = (float)Minecraft.getMinecraft().mouseHelper.deltaY * f1;
            int i = 1;

            if (Minecraft.getMinecraft().gameSettings.invertMouse)
            {
                i = -1;
            }

            if (Minecraft.getMinecraft().gameSettings.thirdPersonView > 0)
            {
                i = 1;
            }

            if (Minecraft.getMinecraft().gameSettings.smoothCamera)
            {
                float smoothCamYaw = (Float) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamYaw", "field_78496_H"}, entityRenderer) + f2;
                float smoothCamPitch = (Float) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamPitch", "field_78497_I"}, entityRenderer) + f3;
                float smoothCamPartialTicks = (Float) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamPartialTicks", "field_78492_L"}, entityRenderer);
                float f4 = partialTicks - smoothCamPartialTicks;
                setObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamPartialTicks", "field_78492_L"}, entityRenderer, partialTicks);
                float smoothCamFilterX = (Float) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamFilterX", "field_78501_S"}, entityRenderer);
                float smoothCamFilterY = (Float) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamFilterY", "field_78502_T"}, entityRenderer);
                f2 = smoothCamFilterX * f4;
                f3 = smoothCamFilterY * f4;
                setObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamYaw", "field_78496_H"}, entityRenderer, smoothCamYaw);
                setObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamPitch", "field_78497_I"}, entityRenderer, smoothCamPitch);
                if (Meta.slientAimEnabled) {
                    rotMng.rendererViewEntity.setAngles(f2, f3 * (float) i);
                    slient = true;
                }
                else {
                    if (slient) {
                        Minecraft.getMinecraft().thePlayer.rotationPitch = Hooks.rotMng.rendererViewEntity.rotationPitch;
                        Minecraft.getMinecraft().thePlayer.rotationYaw = Hooks.rotMng.rendererViewEntity.rotationYaw;
                        Minecraft.getMinecraft().thePlayer.prevRotationPitch = Hooks.rotMng.rendererViewEntity.prevRotationPitch;
                        Minecraft.getMinecraft().thePlayer.prevRotationYaw = Hooks.rotMng.rendererViewEntity.prevRotationYaw;
                        slient = false;
                    }
                    Minecraft.getMinecraft().getRenderViewEntity().setAngles(f2, f3 * (float) i);
                    rotMng.rendererViewEntity.prevRotationYaw = rotMng.rendererViewEntity.rotationYaw = Minecraft.getMinecraft().getRenderViewEntity().rotationYaw;
                    rotMng.rendererViewEntity.prevRotationPitch = rotMng.rendererViewEntity.rotationPitch = Minecraft.getMinecraft().getRenderViewEntity().rotationPitch;
                }
            }
            else
            {
                setObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamYaw", "field_78496_H"}, entityRenderer, 0.0F);
                setObfuscatedFieldValue(EntityRenderer.class, new String[]{"smoothCamPitch", "field_78497_I"}, entityRenderer, 0.0F);
                if (Meta.slientAimEnabled) {
                    rotMng.rendererViewEntity.setAngles(f2, f3 * (float) i);
                    slient = true;
                }
                else {
                    if (slient) {
                        Minecraft.getMinecraft().thePlayer.rotationPitch = Hooks.rotMng.rendererViewEntity.rotationPitch;
                        Minecraft.getMinecraft().thePlayer.rotationYaw = Hooks.rotMng.rendererViewEntity.rotationYaw;
                        Minecraft.getMinecraft().thePlayer.prevRotationPitch = Hooks.rotMng.rendererViewEntity.prevRotationPitch;
                        Minecraft.getMinecraft().thePlayer.prevRotationYaw = Hooks.rotMng.rendererViewEntity.prevRotationYaw;
                        slient = false;
                    }
                    Minecraft.getMinecraft().getRenderViewEntity().setAngles(f2, f3 * (float) i);
                    rotMng.rendererViewEntity.prevRotationYaw = rotMng.rendererViewEntity.rotationYaw = Minecraft.getMinecraft().getRenderViewEntity().rotationYaw;
                    rotMng.rendererViewEntity.prevRotationPitch = rotMng.rendererViewEntity.rotationPitch = Minecraft.getMinecraft().getRenderViewEntity().rotationPitch;
                }
            }
        }

        Minecraft.getMinecraft().mcProfiler.endSection();

        if (!Minecraft.getMinecraft().skipRenderWorld)
        {
            entityRenderer.anaglyphEnable = Minecraft.getMinecraft().gameSettings.anaglyph;
            final ScaledResolution scaledresolution = new ScaledResolution(Minecraft.getMinecraft());
            int i1 = scaledresolution.getScaledWidth();
            int j1 = scaledresolution.getScaledHeight();
            final int k1 = Mouse.getX() * i1 / Minecraft.getMinecraft().displayWidth;
            final int l1 = j1 - Mouse.getY() * j1 / Minecraft.getMinecraft().displayHeight - 1;
            int i2 = Minecraft.getMinecraft().gameSettings.limitFramerate;

            if (Minecraft.getMinecraft().theWorld != null)
            {
                Minecraft.getMinecraft().mcProfiler.startSection("level");
                int j = Math.min(Minecraft.getDebugFPS(), i2);
                j = Math.max(j, 60);
                long k = System.nanoTime() - nanoTime;
                long l = Math.max((long)(1000000000 / j / 4) - k, 0L);
                entityRenderer.renderWorld(partialTicks, System.nanoTime() + l);

                if (OpenGlHelper.shadersSupported)
                {
                    Minecraft.getMinecraft().renderGlobal.renderEntityOutlineFramebuffer();

                    Object theShaderGroup = getObfuscatedFieldValue(EntityRenderer.class, new String[]{"theShaderGroup", "field_147707_d"}, entityRenderer);
                    boolean useShader = (Boolean) getObfuscatedFieldValue(EntityRenderer.class, new String[]{"useShader", "field_175083_ad"}, entityRenderer);
                    if (theShaderGroup != null && useShader)
                    {
                        GlStateManager.matrixMode(5890);
                        GlStateManager.pushMatrix();
                        GlStateManager.loadIdentity();
                        invokeObfuscatedMethod(theShaderGroup.getClass(), new String[]{"loadShaderGroup", "func_148018_a"}, theShaderGroup, new Class[]{float.class}, new Object[]{partialTicks});
                        GlStateManager.popMatrix();
                    }

                    Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
                }

                setObfuscatedFieldValue(EntityRenderer.class, new String[]{"renderEndNanoTime", "field_78510_Z"}, entityRenderer, System.nanoTime());
                Minecraft.getMinecraft().mcProfiler.endStartSection("gui");

                if (!Minecraft.getMinecraft().gameSettings.hideGUI || Minecraft.getMinecraft().currentScreen != null)
                {
                    GlStateManager.alphaFunc(516, 0.1F);
                    Minecraft.getMinecraft().ingameGUI.renderGameOverlay(partialTicks);
                }

                Minecraft.getMinecraft().mcProfiler.endSection();
            }
            else
            {
                GlStateManager.viewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();
                entityRenderer.setupOverlayRendering();
                setObfuscatedFieldValue(EntityRenderer.class, new String[]{"renderEndNanoTime", "field_78510_Z"}, entityRenderer, System.nanoTime());
            }

            if (Minecraft.getMinecraft().currentScreen != null)
            {
                GlStateManager.clear(256);

                try
                {
                    Minecraft.getMinecraft().currentScreen.drawScreen(k1, l1, partialTicks);
                }
                catch (Throwable throwable)
                {
                    CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Rendering screen");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Screen render details");
                    crashreportcategory.addCrashSectionCallable("Screen name", new Callable<String>()
                    {
                        public String call() throws Exception
                        {
                            return Minecraft.getMinecraft().currentScreen.getClass().getCanonicalName();
                        }
                    });
                    crashreportcategory.addCrashSectionCallable("Mouse location", new Callable<String>()
                    {
                        public String call() throws Exception
                        {
                            return String.format("Scaled: (%d, %d). Absolute: (%d, %d)", new Object[] {Integer.valueOf(k1), Integer.valueOf(l1), Integer.valueOf(Mouse.getX()), Integer.valueOf(Mouse.getY())});
                        }
                    });
                    crashreportcategory.addCrashSectionCallable("Screen size", new Callable<String>()
                    {
                        public String call() throws Exception
                        {
                            return String.format("Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d", new Object[] {Integer.valueOf(scaledresolution.getScaledWidth()), Integer.valueOf(scaledresolution.getScaledHeight()), Integer.valueOf(Minecraft.getMinecraft().displayWidth), Integer.valueOf(Minecraft.getMinecraft().displayHeight), Integer.valueOf(scaledresolution.getScaleFactor())});
                        }
                    });
                    throw new ReportedException(crashreport);
                }
            }
        }
    }

    public static void updatePlayerMoveState() {
        Minecraft mc = Minecraft.getMinecraft();
        float yawDiff = rotMng.normalizeAngle(rotMng.rendererViewEntity.rotationYaw - mc.thePlayer.rotationYaw);
        float pitchDiff = rotMng.normalizeAngle(rotMng.rendererViewEntity.rotationPitch - mc.thePlayer.rotationPitch);
        MovementInput mvInput = mc.thePlayer.movementInput;
        mvInput.moveStrafe = 0.0F;
        mvInput.moveForward = 0.0F;

        if (mc.gameSettings.keyBindForward.isKeyDown())
        {
            ++mvInput.moveForward;
        }

        if (mc.gameSettings.keyBindBack.isKeyDown())
        {
            --mvInput.moveForward;
        }

        if (mc.gameSettings.keyBindLeft.isKeyDown())
        {
            ++mvInput.moveStrafe;
        }

        if (mc.gameSettings.keyBindRight.isKeyDown())
        {
            --mvInput.moveStrafe;
        }

        mvInput.jump = mc.gameSettings.keyBindJump.isKeyDown();
        mvInput.sneak = mc.gameSettings.keyBindSneak.isKeyDown();

        if (mc.thePlayer != null && rotMng.rendererViewEntity != null) {
            transformMovementInput(mvInput, yawDiff);
        }

        if (mvInput.sneak)
        {
            mvInput.moveStrafe = (float)((double)mvInput.moveStrafe * 0.3D);
            mvInput.moveForward = (float)((double)mvInput.moveForward * 0.3D);
        }
    }
    
    public static void transformMovementInput(MovementInput mvInput, float yawDiff) {
        float moveForward = mvInput.moveForward;
        float moveStrafe = mvInput.moveStrafe;
        
        float yawRad = (float) Math.toRadians(yawDiff);
        float cos = MathHelper.cos(yawRad);
        float sin = MathHelper.sin(-yawRad);
        float newMoveForward = moveForward * cos - moveStrafe * sin;
        float newMoveStrafe = moveForward * sin + moveStrafe * cos;
        final float THRESHOLD = 0.1f;
        
        if (Math.abs(newMoveForward) > 1.0f + THRESHOLD || Math.abs(newMoveStrafe) > 1.0f + THRESHOLD) {
            float length = MathHelper.sqrt_float(newMoveForward * newMoveForward + newMoveStrafe * newMoveStrafe);
            if (length > THRESHOLD) {
                newMoveForward /= length;
                newMoveStrafe /= length;
            }
        }
        
        mvInput.moveForward = applySmoothSign(newMoveForward, THRESHOLD);
        mvInput.moveStrafe = applySmoothSign(newMoveStrafe, THRESHOLD);
    }
    
    private static float applySmoothSign(float value, float threshold) {
        if (value > threshold) return 1.0f;
        if (value < -threshold) return -1.0f;
        return 0.0f;
    }
    
    static {
        logger.info("Hooks class initialized by classloader: {}", Hooks.class.getClassLoader());
    }
}