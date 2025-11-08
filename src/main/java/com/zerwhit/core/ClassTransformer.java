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
            
            HookConfig.HookEntry hookConfig = HookConfig.getHookConfig(className, name, desc);
            if (hookConfig != null) {
                return createHookMethodVisitor(mv, hookConfig);
            }
            
            return mv;
        }

        private MethodVisitor createHookMethodVisitor(MethodVisitor mv, HookConfig.HookEntry hookConfig) {
            switch (hookConfig.hookType) {
                case BEFORE:
                    return new UniversalHookMethodVisitor(mv, hookConfig, true, false);
                case AFTER:
                    return new UniversalHookMethodVisitor(mv, hookConfig, false, true);
                case REPLACE:
                    if (hookConfig.hookMethod.equals("renderItemInFirstPersonHook")) {
                        return new ItemRendererHookMethodVisitor(mv);
                    }
                    break;
                case CONDITIONAL:
                    return new ConditionalHookMethodVisitor(mv, hookConfig);
            }
            
            return mv;
        }
    }

    private static class UniversalHookMethodVisitor extends MethodVisitor {
        private final HookConfig.HookEntry hookConfig;
        private final boolean callBefore;
        private final boolean callAfter;

        public UniversalHookMethodVisitor(MethodVisitor mv, HookConfig.HookEntry hookConfig, boolean callBefore, boolean callAfter) {
            super(Opcodes.ASM4, mv);
            this.hookConfig = hookConfig;
            this.callBefore = callBefore;
            this.callAfter = callAfter;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (callBefore && hookConfig != null) {
                if (hookConfig.parameters != null && hookConfig.parameters.length > 0) {
                    loadParameters();
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    getClassPackage(Hooks.class), 
                    hookConfig.hookMethod, 
                    hookConfig.getHookMethodDescriptor());
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN && callAfter && hookConfig != null) {
                if (hookConfig.parameters != null && hookConfig.parameters.length > 0) {
                    loadParameters();
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                    getClassPackage(Hooks.class), 
                    hookConfig.hookMethod, 
                    hookConfig.getHookMethodDescriptor());
            }
            super.visitInsn(opcode);
        }
        
        private void loadParameters() {
            if (hookConfig == null || hookConfig.parameters == null) {
                return;
            }
            
            for (int i = 0; i < hookConfig.parameters.length; i++) {
                String paramType = hookConfig.parameters[i];
                int paramIndex = hookConfig.getParameterIndex(i);
                
                switch (paramType) {
                    case "F":
                        mv.visitVarInsn(Opcodes.FLOAD, paramIndex);
                        break;
                    case "I":
                        mv.visitVarInsn(Opcodes.ILOAD, paramIndex);
                        break;
                    case "D":
                        mv.visitVarInsn(Opcodes.DLOAD, paramIndex);
                        break;
                    case "J":
                        mv.visitVarInsn(Opcodes.LLOAD, paramIndex);
                        break;
                    case "Z":
                        mv.visitVarInsn(Opcodes.ILOAD, paramIndex);
                        break;
                    default:
                        mv.visitVarInsn(Opcodes.ALOAD, paramIndex);
                        break;
                }
            }
        }
        

    }



    private static class ItemRendererHookMethodVisitor extends MethodVisitor {
        public ItemRendererHookMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }

        @Override
        public void visitCode() {
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    getClassPackage(Hooks.class),
                    "renderItemInFirstPersonHook",
                    "(F)V");
            mv.visitFieldInsn(Opcodes.GETSTATIC, getClassPackage(Meta.class), "legacyAnimEnabled", "Z");
            Label skipReturn = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, skipReturn);
            Label label = new Label();
            mv.visitLabel(label);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitLabel(skipReturn);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mv.visitMaxs(2, 2);
        }
    }

    private static class ConditionalHookMethodVisitor extends MethodVisitor {
        private final HookConfig.HookEntry hookConfig;
        private final Label skipOriginalLabel = new Label();

        public ConditionalHookMethodVisitor(MethodVisitor mv, HookConfig.HookEntry hookConfig) {
            super(Opcodes.ASM4, mv);
            this.hookConfig = hookConfig;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            
            if (hookConfig.parameters != null && hookConfig.parameters.length > 0) {
                loadParameters();
            }
            
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                getClassPackage(Hooks.class), 
                hookConfig.hookMethod, 
                hookConfig.getHookMethodDescriptor());
            
            mv.visitJumpInsn(Opcodes.IFEQ, skipOriginalLabel);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                mv.visitLabel(skipOriginalLabel);
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mv.visitMaxs(Math.max(maxStack, 2), maxLocals);
        }
        
        private void loadParameters() {
            if (hookConfig == null || hookConfig.parameters == null) {
                return;
            }
            
            for (int i = 0; i < hookConfig.parameters.length; i++) {
                String paramType = hookConfig.parameters[i];
                int paramIndex = hookConfig.getParameterIndex(i);
                
                switch (paramType) {
                    case "F":
                        mv.visitVarInsn(Opcodes.FLOAD, paramIndex);
                        break;
                    case "I":
                        mv.visitVarInsn(Opcodes.ILOAD, paramIndex);
                        break;
                    case "D":
                        mv.visitVarInsn(Opcodes.DLOAD, paramIndex);
                        break;
                    case "J":
                        mv.visitVarInsn(Opcodes.LLOAD, paramIndex);
                        break;
                    case "Z":
                        mv.visitVarInsn(Opcodes.ILOAD, paramIndex);
                        break;
                    default:
                        mv.visitVarInsn(Opcodes.ALOAD, paramIndex);
                        break;
                }
            }
        }
    }

    public static String getClassPackage(Class<?> klass) {
        return klass.getName().replace('.', '/');
    }
}