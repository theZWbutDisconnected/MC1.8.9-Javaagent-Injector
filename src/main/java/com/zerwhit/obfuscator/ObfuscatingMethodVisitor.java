package com.zerwhit.obfuscator;

import com.zerwhit.core.ClassTransformer;
import com.zerwhit.obfuscator.parser.CsvParser;
import com.zerwhit.obfuscator.parser.TsrgParser;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class ObfuscatingMethodVisitor extends MethodVisitor {
    private String ownerClassName;
    private String originalMethodName;
    private String obfuscatedMethodName;
    private TsrgParser tsrgParser;
    private CsvParser csvParser;
    private boolean extendsMinecraftClass;
    private String superClassName;
    private boolean implementsMinecraftInterface;
    private String[] interfaces;

    public ObfuscatingMethodVisitor(MethodVisitor mv, String ownerClassName,
                                    String originalMethodName, String obfuscatedMethodName,
                                    TsrgParser tsrgParser, CsvParser csvParser,
                                    boolean extendsMinecraftClass, String superClassName,
                                    boolean implementsMinecraftInterface, String[] interfaces) {
        super(Opcodes.ASM9, mv);
        this.ownerClassName = ownerClassName;
        this.originalMethodName = originalMethodName;
        this.obfuscatedMethodName = obfuscatedMethodName;
        this.tsrgParser = tsrgParser;
        this.csvParser = csvParser;
        this.extendsMinecraftClass = extendsMinecraftClass;
        this.superClassName = superClassName;
        this.implementsMinecraftInterface = implementsMinecraftInterface;
        this.interfaces = interfaces;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean ift) {
        String mappedOwner = tsrgParser.getMappedClass(owner);
        String mappedName = getMappedMethodName(owner, name, descriptor, opcode);

        if (!name.equals(mappedName)) {
            System.out.println("Mapping Method: " + owner + "." + name + descriptor + " -> " + mappedOwner + "." + mappedName + descriptor);
        }

        super.visitMethodInsn(opcode, mappedOwner, mappedName, descriptor, ift);
    }

    private String getMappedMethodName(String owner, String name, String descriptor, int opcode) {
        String mappedName = tsrgParser.getMappedMethod(owner, name, descriptor);

        if (mappedName == null || mappedName.equals(name)) {
            // 处理继承的方法调用
            if (extendsMinecraftClass && superClassName != null && 
                (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL) &&
                owner.equals(ownerClassName) && name.equals(originalMethodName)) {
                // 如果是调用父类的重写方法，使用父类的映射
                String parentMappedName = tsrgParser.getMappedMethod(superClassName, name, descriptor);
                if (!parentMappedName.equals(name)) {
                    return parentMappedName;
                }
            }
            
            // 处理接口方法的调用
            if (implementsMinecraftInterface && interfaces != null && 
                (opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL) &&
                owner.equals(ownerClassName) && name.equals(originalMethodName)) {
                // 如果是调用接口的实现方法，尝试从接口获取映射
                for (String iface : interfaces) {
                    if (isMinecraftClass(iface)) {
                        String interfaceMappedName = tsrgParser.getMappedMethod(iface, name, descriptor);
                        if (!interfaceMappedName.equals(name)) {
                            return interfaceMappedName;
                        }
                    }
                }
            }
            
            if (name.equals(originalMethodName) && owner.equals(ownerClassName)) {
                mappedName = obfuscatedMethodName;
            } else {
                mappedName = name;
            }
        }

        return ClassTransformer.isMCClass(owner) ? csvParser.methodMappings.getOrDefault(mappedName, mappedName) : mappedName;
    }

    private boolean isMinecraftClass(String className) {
        if (className == null) return false;
        return className.startsWith("net/minecraft") || 
               className.startsWith("com/mojang") || 
               className.startsWith("badlion");
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        String mappedOwner = tsrgParser.getMappedClass(owner);
        String mappedName = tsrgParser.getMappedField(owner, name);

        if (!name.equals(mappedName)) {
            System.out.println("Mapping Field: " + owner + "." + name + " -> " + mappedOwner + "." + mappedName);
        }
        super.visitFieldInsn(opcode, mappedOwner, ClassTransformer.isMCClass(mappedOwner) ? csvParser.fieldMappings.getOrDefault(mappedName, mappedName) : mappedName, descriptor);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        String mappedType = tsrgParser.getMappedClass(type);

        if (!type.equals(mappedType)) {
            System.out.println("Mapping Type Include: " + type + " -> " + mappedType);
        }

        super.visitTypeInsn(opcode, mappedType);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        super.visitFrame(type, nLocal, mapFrameTypes(local), nStack, mapFrameTypes(stack));
    }

    private Object[] mapFrameTypes(Object[] types) {
        if (types == null) return null;

        Object[] mapped = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] instanceof String) {
                mapped[i] = tsrgParser.getMappedClass((String) types[i]);
            } else {
                mapped[i] = types[i];
            }
        }
        return mapped;
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature,
                                   Label start, Label end, int index) {
        String mappedName = "var" + index;
        super.visitLocalVariable(mappedName, descriptor, signature, start, end, index);
    }
}