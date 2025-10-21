package com.zerwhit;

import com.zerwhit.core.Hooks;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class ClassTransformer implements ClassFileTransformer {
    private static final String[] SYSTEM_PACKAGES = new String[]{"java/", "sun/", "com/sun/", "jdk/", "javax/"};
    private static final String[] MINECRAFT_PACKAGES = new String[]{
            "net/minecraft", "com/mojang", "badlion"
    };

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        if (className == null || isSystemClass(className)) {
            return classfileBuffer;
        }

        if (isMCClass(className)) {
//            System.out.println("Transforming: " + className);
            return transformMinecraftClass(className, classfileBuffer);
        } else {
            return classfileBuffer;
        }
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
            e.printStackTrace();
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
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            switch (className) {
                case "net/minecraft/client/Minecraft" :
                    return transformMinecraftMethod(mv, name, desc);
                case "net/minecraft/entity/player/EntityPlayer" :
                    return transformPlayerMethod(mv, name, desc);
                default : return mv;
            }
        }

        private MethodVisitor transformMinecraftMethod(MethodVisitor mv, String name, String desc) {
            switch (name + desc) {
                case "func_71411_J()V" :
                case "runGameLoop()V" :
                    return new GameLoopMethodVisitor(mv);
                case "func_71407_l()V" :
                case "runTick()V" : return new RunTickMethodVisitor(mv);
                default : return mv;
            }
        }

        private MethodVisitor transformPlayerMethod(MethodVisitor mv, String name, String desc) {
            switch (name + desc) {
                case "onUpdate()V" : return new PlayerUpdateMethodVisitor(mv);
                default : return mv;
            }
        }
    }

    private abstract static class HookMethodVisitor extends MethodVisitor {
        public HookMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }

        protected void insertBefore() {}
        protected void insertAfter() {}

        @Override
        public void visitCode() {
            super.visitCode();
            insertBefore();
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                insertAfter();
            }
            super.visitInsn(opcode);
        }
    }

    private static class GameLoopMethodVisitor extends HookMethodVisitor {
        public GameLoopMethodVisitor(MethodVisitor mv) {
            super(mv);
        }

        @Override
        protected void insertBefore() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassPackage(Hooks.class), "onGameLoop", "()V");
        }
    }

    private static class RunTickMethodVisitor extends HookMethodVisitor {
        public RunTickMethodVisitor(MethodVisitor mv) {
            super(mv);
        }

        @Override
        protected void insertBefore() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassPackage(Hooks.class), "onPreTick", "()V");
        }

        @Override
        protected void insertAfter() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassPackage(Hooks.class), "onPostTick", "()V");
        }
    }

    private static class PlayerUpdateMethodVisitor extends HookMethodVisitor {
        public PlayerUpdateMethodVisitor(MethodVisitor mv) {
            super(mv);
        }

        @Override
        protected void insertBefore() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassPackage(Hooks.class), "onPlayerPreUpdate", "()V");
        }

        @Override
        protected void insertAfter() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, getClassPackage(Hooks.class), "onPlayerPostUpdate", "()V");
        }
    }

    public static String getClassPackage(Class klass) {
        return klass.getName().replace('.', '/');
    }
}