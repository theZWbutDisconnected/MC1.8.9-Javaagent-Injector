package com.zerwhit;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class ObfuscatingMethodVisitor extends MethodVisitor {
    private String ownerClassName;
    private String originalMethodName;
    private String obfuscatedMethodName;
    private TsrgParser tsrgParser;
    private CsvParser csvParser;

    public ObfuscatingMethodVisitor(MethodVisitor mv, String ownerClassName,
                                    String originalMethodName, String obfuscatedMethodName,
                                    TsrgParser tsrgParser, CsvParser csvParser) {
        super(Opcodes.ASM9, mv);
        this.ownerClassName = ownerClassName;
        this.originalMethodName = originalMethodName;
        this.obfuscatedMethodName = obfuscatedMethodName;
        this.tsrgParser = tsrgParser;
        this.csvParser = csvParser;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean ift) {
        String mappedOwner = tsrgParser.getMappedClass(owner);
        String mappedName = tsrgParser.getMappedMethod(owner, name, descriptor);

        if (mappedName == null || mappedName.equals(name)) {
            if (name.equals(originalMethodName) && owner.equals(ownerClassName)) {
                mappedName = obfuscatedMethodName;
            } else {
                mappedName = name;
            }
        }

        if (!name.equals(mappedName)) {
            System.out.println("Mapping Method: " + owner + "." + name + descriptor + " -> " + mappedOwner + "." + mappedName + descriptor);
        }

        super.visitMethodInsn(opcode, mappedOwner, ClassTransformer.isMCClass(mappedOwner) ? csvParser.methodMappings.getOrDefault(mappedName, mappedName) : mappedName, descriptor, ift);
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