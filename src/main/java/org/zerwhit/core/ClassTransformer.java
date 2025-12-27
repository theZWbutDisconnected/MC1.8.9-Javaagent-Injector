package org.zerwhit.core;

import org.zerwhit.core.data.HookConfig;
import org.zerwhit.core.data.Meta;
import org.zerwhit.core.obfuscation.DeobfuscationRemapper;
import org.zerwhit.core.util.SafeLogger;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class ClassTransformer implements ClassFileTransformer {
    
    private static final SafeLogger LOGGER = SafeLogger.getLogger(ClassTransformer.class);
    private static final int WRITER_FLAGS = ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES;
    private static final int READER_FLAGS = ClassReader.EXPAND_FRAMES;
    
    private static final class Constants {
        private static final String[] SYSTEM_PACKAGES = {"java/", "sun/", "com/sun/", "jdk/", "javax/"};
        private static final String[] MINECRAFT_PACKAGES = {"net/minecraft", "com/mojang", "badlion"};
        
        private Constants() {
            throw new AssertionError("Cannot instantiate constants class");
        }
    }
    
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        LOGGER.info("ClassLoader: {}, ClassName: {}", loader, className);
        
        if (className == null || isSystemClass(className) || !isMCClass(className)) {
            return classfileBuffer;
        }
        
        return transformMinecraftClass(className, classfileBuffer);
    }

    public static boolean isSystemClass(String className) {
        if (className == null) {
            return false;
        }
        
        for (String pkg : Constants.SYSTEM_PACKAGES) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean isMCClass(String className) {
        if (className == null) {
            return false;
        }
        
        for (String pkg : Constants.MINECRAFT_PACKAGES) {
            if (className.startsWith(pkg) || !className.contains("/")) {
                return true;
            }
        }
        
        return false;
    }
    
    private byte[] transformMinecraftClass(String className, byte[] classfileBuffer) {
        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            ClientClassObfuscator clientVisitor = new ClientClassObfuscator(writer, className);
            MinecraftClassVisitor minecraftVisitor = new MinecraftClassVisitor(clientVisitor, className);
            
            reader.accept(minecraftVisitor, READER_FLAGS);
            return writer.toByteArray();
        } catch (Exception e) {
            LOGGER.error("Failed to transform class: {}", className, e);
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
                    return createReplaceHookMethodVisitor(mv, hookConfig);
                case CONDITIONAL:
                    return new ConditionalHookMethodVisitor(mv, hookConfig);
                default:
                    return mv;
            }
        }
        
        private MethodVisitor createReplaceHookMethodVisitor(MethodVisitor mv, HookConfig.HookEntry hookConfig) {
            String hookMethod = hookConfig.hookMethod;
            
            if ("renderItemInFirstPersonHook".equals(hookMethod)) {
                return new ItemRendererHookMethodVisitor(mv);
            }
            
            if ("orientCameraHook".equals(hookMethod)) {
                return new EntityRendererOrientCameraHookMethodVisitor(mv);
            }

            if ("updateCameraAndRenderHook".equals(hookMethod)) {
                return new EntityRendererCameraHookMethodVisitor(mv);
            }

            if ("updatePlayerMoveState".equals(hookMethod)) {
                return new MovementInputHookMethodVisitor(mv);
            }
            
            return mv;
        }
    }
    
    private static class ClientClassObfuscator extends ClassVisitor {
        private final String className;
        private final DeobfuscationRemapper deobfuscationMapping;

        public ClientClassObfuscator(ClassVisitor cv, String className) {
            super(Opcodes.ASM4, cv);
            this.className = className;
            this.deobfuscationMapping = DeobfuscationRemapper.getInstance();
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            String deobfuscatedName = deobfuscationMapping.getDeobfuscatedClassName(name);
            String deobfuscatedSuperName = superName != null ? deobfuscationMapping.getDeobfuscatedClassName(superName) : superName;
            String[] deobfuscatedInterfaces = interfaces != null ? new String[interfaces.length] : interfaces;
            
            if (deobfuscatedInterfaces != null) {
                for (int i = 0; i < interfaces.length; i++) {
                    deobfuscatedInterfaces[i] = deobfuscationMapping.getDeobfuscatedClassName(interfaces[i]);
                }
            }
            
            super.visit(version, access, deobfuscatedName, signature, deobfuscatedSuperName, deobfuscatedInterfaces);
        }
        
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            String deobfuscatedFieldName = deobfuscationMapping.getDeobfuscatedFieldName(className, name);
            return super.visitField(access, deobfuscatedFieldName, descriptor, signature, value);
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            String deobfuscatedMethodName = deobfuscationMapping.getDeobfuscatedMethodName(className, name);
            return super.visitMethod(access, deobfuscatedMethodName, descriptor, signature, exceptions);
        }
    }
    
    private static class UniversalHookMethodVisitor extends MethodVisitor {
        private final HookConfig.HookEntry hookConfig;
        private final boolean callBefore;
        private final boolean callAfter;
        
        public UniversalHookMethodVisitor(MethodVisitor mv, HookConfig.HookEntry hookConfig, 
                                         boolean callBefore, boolean callAfter) {
            super(Opcodes.ASM4, mv);
            this.hookConfig = hookConfig;
            this.callBefore = callBefore;
            this.callAfter = callAfter;
        }
        
        @Override
        public void visitCode() {
            super.visitCode();
            
            if (callBefore && hookConfig != null) {
                callHookMethod();
            }
        }
        
        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN && callAfter && hookConfig != null) {
                callHookMethod();
            }
            
            super.visitInsn(opcode);
        }
        
        private void callHookMethod() {
            if (hookConfig.parameters != null && hookConfig.parameters.length > 0) {
                loadParameters();
            }
            
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
                getClassPackage(Hooks.class), 
                hookConfig.hookMethod, 
                hookConfig.getHookMethodDescriptor());
        }
        
        private void loadParameters() {
            if (hookConfig == null || hookConfig.parameters == null) {
                return;
            }
            
            for (int i = 0; i < hookConfig.parameters.length; i++) {
                String paramType = hookConfig.parameters[i];
                int paramIndex = hookConfig.getParameterIndex(i);
                
                loadParameter(paramType, paramIndex);
            }
        }
        
        private void loadParameter(String paramType, int paramIndex) {
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

    private static class MovementInputHookMethodVisitor extends MethodVisitor {
        public MovementInputHookMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }

        @Override
        public void visitCode() {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    getClassPackage(Hooks.class),
                    "updatePlayerMoveState",
                    "()V");
            mv.visitInsn(Opcodes.RETURN);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mv.visitMaxs(2, 2);
        }
    }
    
    private static class EntityRendererOrientCameraHookMethodVisitor extends MethodVisitor {
        public EntityRendererOrientCameraHookMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }
        
        @Override
        public void visitCode() {
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    getClassPackage(Hooks.class),
                    "orientCameraHook",
                    "(F)V");
            mv.visitInsn(Opcodes.RETURN);
        }
        
        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            mv.visitMaxs(2, 2);
        }
    }
    
    private static class EntityRendererCameraHookMethodVisitor extends MethodVisitor {
        public EntityRendererCameraHookMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM4, mv);
        }
        
        @Override
        public void visitCode() {
            mv.visitVarInsn(Opcodes.FLOAD, 1);
            mv.visitVarInsn(Opcodes.LLOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    getClassPackage(Hooks.class),
                    "updateCameraAndRenderHook",
                    "(FJ)V");
            mv.visitInsn(Opcodes.RETURN);
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
                
                loadParameter(paramType, paramIndex);
            }
        }
        
        private void loadParameter(String paramType, int paramIndex) {
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
    
    public static String getClassPackage(Class<?> klass) {
        return klass.getName().replace('.', '/');
    }
}