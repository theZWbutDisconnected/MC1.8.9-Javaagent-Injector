package com.zerwhit;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class ObfuscatingClassVisitor extends ClassVisitor {
    private String className;
    private String obfuscatedClassName;
    private TsrgParser tsrgParser;
    private CsvParser csvParser;
    private String superClassName;
    private boolean extendsMinecraftClass = false;
    private String[] interfaces;
    private boolean implementsMinecraftInterface = false;

    public ObfuscatingClassVisitor(ClassVisitor cv, String className, TsrgParser tsrgParser, CsvParser csvParser) {
        super(Opcodes.ASM9, cv);
        this.className = className;
        this.tsrgParser = tsrgParser;
        this.csvParser = csvParser;
        this.obfuscatedClassName = tsrgParser.getMappedClass(className);

        if (!className.equals(obfuscatedClassName)) {
            System.out.println("Class Mapping: " + className + " -> " + obfuscatedClassName);
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        this.superClassName = superName;
        this.interfaces = interfaces;
        this.extendsMinecraftClass = isMinecraftClass(superName);
        this.implementsMinecraftInterface = hasMinecraftInterface(interfaces);
        
        String mappedSuperName = superName != null ? tsrgParser.getMappedClass(superName) : superName;
        String[] mappedInterfaces = mapInterfaces(interfaces);

        super.visit(version, access, obfuscatedClassName, signature, mappedSuperName, mappedInterfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
                                   String signature, Object value) {
        String obfuscatedFieldName = tsrgParser.getMappedField(className, name);

        if (!name.equals(obfuscatedFieldName)) {
            System.out.println("Field Mapping: " + className + "." + name + " -> " + obfuscatedFieldName);
        }

        return super.visitField(access, obfuscatedFieldName, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        if (isExcludedMethod(name)) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        String obfuscatedMethodName = getMappedMethodName(access, name, descriptor);

        if (!name.equals(obfuscatedMethodName)) {
            System.out.println("Method Mapping: " + className + "." + name + descriptor + " -> " + obfuscatedMethodName);
        }

        return new ObfuscatingMethodVisitor(
                super.visitMethod(access, obfuscatedMethodName, descriptor, signature, exceptions),
                className,
                name,
                obfuscatedMethodName,
                tsrgParser,
                csvParser,
                extendsMinecraftClass,
                superClassName,
                implementsMinecraftInterface,
                interfaces
        );
    }

    private String getMappedMethodName(int access, String name, String descriptor) {
        String mappedName = tsrgParser.getMappedMethod(className, name, descriptor);
        
        if (mappedName.equals(name) && extendsMinecraftClass && superClassName != null) {
            String parentMappedName = tsrgParser.getMappedMethod(superClassName, name, descriptor);
            if (!parentMappedName.equals(name)) {
                return parentMappedName;
            }
        }
        if (mappedName.equals(name) && implementsMinecraftInterface && interfaces != null) {
            for (String iface : interfaces) {
                if (isMinecraftClass(iface)) {
                    String interfaceMappedName = tsrgParser.getMappedMethod(iface, name, descriptor);
                    if (!interfaceMappedName.equals(name)) {
                        return interfaceMappedName;
                    }
                }
            }
        }
        
        return mappedName;
    }

    private boolean isMinecraftClass(String className) {
        if (className == null) return false;
        return className.startsWith("net/minecraft") || 
               className.startsWith("com/mojang") || 
               className.startsWith("badlion");
    }

    private boolean hasMinecraftInterface(String[] interfaces) {
        if (interfaces == null) return false;
        for (String iface : interfaces) {
            if (isMinecraftClass(iface)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        String mappedName = tsrgParser.getMappedClass(name);
        String mappedOuterName = outerName != null ? tsrgParser.getMappedClass(outerName) : null;
        super.visitInnerClass(mappedName, mappedOuterName, innerName, access);
    }

    private boolean isExcludedMethod(String methodName) {
        return methodName.equals("<init>") || methodName.equals("<clinit>") ||
               methodName.equals("main") || methodName.equals("premain") || methodName.equals("agentmain");
    }

    private String[] mapInterfaces(String[] interfaces) {
        if (interfaces == null) return null;

        String[] mappedInterfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            mappedInterfaces[i] = tsrgParser.getMappedClass(interfaces[i]);
        }
        return mappedInterfaces;
    }
}