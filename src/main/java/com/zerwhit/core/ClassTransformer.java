package com.zerwhit.core;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class ClassTransformer implements ClassFileTransformer {
    private static final String[] SYSTEM_PACKAGES = {"java/", "sun/", "com/sun/", "jdk/", "javax/"};
    private static final String[] MINECRAFT_PACKAGES = {"net/minecraft", "com/mojang", "badlion"};

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className == null || isSystemClass(className) || !isMCClass(className)) {
            return classfileBuffer;
        }
        return transformMinecraftClass(className, classfileBuffer);
    }

    public static boolean isSystemClass(String className) {
        return Arrays.stream(SYSTEM_PACKAGES).anyMatch(className::startsWith);
    }

    public static boolean isMCClass(String className) {
        return Arrays.stream(MINECRAFT_PACKAGES).anyMatch(className::startsWith);
    }

    private byte[] transformMinecraftClass(String className, byte[] classfileBuffer) {
        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            reader.accept(new MinecraftClassVisitor(writer, className), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (Exception e) {
            System.err.println("Failed to transform: " + className);
            return classfileBuffer;
        }
    }

    private static class MinecraftClassVisitor extends ClassVisitor {
        private final String className;

        public MinecraftClassVisitor(ClassVisitor cv, String className) {
            super(Opcodes.ASM4, cv);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            return createHookMethodVisitor(mv, name, desc);
        }

        private MethodVisitor createHookMethodVisitor(MethodVisitor mv, String name, String desc) {
            String methodKey = name + desc;

            switch (className) {
                case "net/minecraft/client/Minecraft":
                    switch (methodKey) {
                        case "func_71411_J()V": case "runGameLoop()V":
                            return new HookMethodVisitor(mv, "onGameLoop");
                        case "func_71407_l()V": case "runTick()V":
                            return new HookMethodVisitor(mv, "onPreTick", "onPostTick");
                        case "func_175601_h()V": case "updateDisplay()V":
                            return new HookMethodVisitor(mv, "onUpdateDisplay");
                    }
                    break;
                case "net/minecraft/entity/player/EntityPlayer":
                    switch (methodKey) {
                        case "func_70071_h_()V": case "onUpdate()V":
                            return new HookMethodVisitor(mv, "onPlayerPreUpdate", "onPlayerPostUpdate");
                        case "func_70097_a(Lnet/minecraft/util/DamageSource;F)Z":
                        case "attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z":
                            return new AutoBlockHookMethodVisitor(mv);
                    }
                    break;
            }
            return mv;
        }
    }

    private static class HookMethodVisitor extends MethodVisitor {
        private final String beforeHook;
        private final String afterHook;

        public HookMethodVisitor(MethodVisitor mv, String hookMethod) {
            this(mv, hookMethod, null);
        }

        public HookMethodVisitor(MethodVisitor mv, String beforeHook, String afterHook) {
            super(Opcodes.ASM4, mv);
            this.beforeHook = beforeHook;
            this.afterHook = afterHook;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (beforeHook != null) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassPackage(Hooks.class), beforeHook, "()V");
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN && afterHook != null) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassPackage(Hooks.class), afterHook, "()V");
            }
            super.visitInsn(opcode);
        }
    }

    private static class AutoBlockHookMethodVisitor extends MethodVisitor {
        public AutoBlockHookMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    getClassPackage(Hooks.class),
                    "onPlayerHurt",
                    "()V");
        }
    }

    public static String getClassPackage(Class<?> klass) {
        return klass.getName().replace('.', '/');
    }
}