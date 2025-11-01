package com.zerwhit.obfuscator;

import com.zerwhit.core.ClassTransformer;
import com.zerwhit.obfuscator.parser.CsvParser;
import com.zerwhit.obfuscator.parser.TsrgParser;
import com.zerwhit.obfuscator.util.ClassHierarchyResolver;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

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
        String mappedName = findMappedMethodInHierarchy(owner, name, descriptor);

        if (mappedName == null || mappedName.equals(name)) {
            if (extendsMinecraftClass && superClassName != null &&
                    (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL) &&
                    owner.equals(ownerClassName) && name.equals(originalMethodName)) {
                String parentMappedName = tsrgParser.getMappedMethod(superClassName, name, descriptor);
                if (!parentMappedName.equals(name)) {
                    return parentMappedName;
                }
            }

            if (implementsMinecraftInterface && interfaces != null &&
                    (opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL) &&
                    owner.equals(ownerClassName) && name.equals(originalMethodName)) {
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

    private String findMappedMethodInHierarchy(String className, String methodName, String descriptor) {
        // First check the current class with descriptor
        String mappedName = tsrgParser.getMappedMethod(className, methodName, descriptor);
        if (!mappedName.equals(methodName)) {
            return mappedName;
        }

        // If not found, check without descriptor
        mappedName = tsrgParser.getMappedMethod(className, methodName);
        if (!mappedName.equals(methodName)) {
            return mappedName;
        }

        // If still not found, traverse the class hierarchy
        List<String> hierarchy = ClassHierarchyResolver.getClassHierarchy(className);
        for (String classInHierarchy : hierarchy) {
            if (classInHierarchy.equals(className)) {
                continue; // Skip the original class as we already checked it
            }

            // Try with descriptor first
            mappedName = tsrgParser.getMappedMethod(classInHierarchy, methodName, descriptor);
            if (!mappedName.equals(methodName)) {
                return mappedName;
            }

            // Try without descriptor
            mappedName = tsrgParser.getMappedMethod(classInHierarchy, methodName);
            if (!mappedName.equals(methodName)) {
                return mappedName;
            }
        }

        return methodName;
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
        String mappedName = findMappedFieldInHierarchy(owner, name);
        if (ClassTransformer.isMCClass(mappedOwner)) {
            mappedName = csvParser.fieldMappings.getOrDefault(mappedName, mappedName);
        }
        if (!name.equals(mappedName)) {
            System.out.println("Mapping Field: " + owner + "." + name + " -> " + mappedOwner + "." + mappedName);
        }
        super.visitFieldInsn(opcode, mappedOwner, mappedName, descriptor);
    }

    private String findMappedFieldInHierarchy(String className, String fieldName) {
        String mappedName = tsrgParser.getMappedField(className, fieldName);
        if (!mappedName.equals(fieldName)) {
            return mappedName;
        }
        List<String> hierarchy = ClassHierarchyResolver.getClassHierarchy(className);
        for (String classInHierarchy : hierarchy) {
            if (classInHierarchy.equals(className)) {
                continue;
            }
            mappedName = tsrgParser.getMappedField(classInHierarchy, fieldName);
            if (!mappedName.equals(fieldName)) {
                return mappedName;
            }
        }

        return fieldName;
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