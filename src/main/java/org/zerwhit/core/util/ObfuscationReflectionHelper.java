package org.zerwhit.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ObfuscationReflectionHelper {
    private static final Logger logger = LogManager.getLogger(ObfuscationReflectionHelper.class);
    
    public static Method getObfuscatedMethod(Class<?> klass, String[] methodNames, Class<?>... parameterTypes) {
        for (Method method : klass.getDeclaredMethods()) {
            boolean hasMethod = Arrays.stream(methodNames).anyMatch((v) -> v.equals(method.getName()));
            
            if (hasMethod) {
                Class<?>[] methodParameterTypes = method.getParameterTypes();
                if (parameterTypes.length == methodParameterTypes.length) {
                    boolean parametersMatch = true;
                    for (int i = 0; i < parameterTypes.length; i++) {
                        if (!isParameterTypeCompatible(parameterTypes[i], methodParameterTypes[i])) {
                            parametersMatch = false;
                            break;
                        }
                    }
                    
                    if (parametersMatch) {
                        try {
                            method.setAccessible(true);
                            return method;
                        } catch (SecurityException e) {
                            return null;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private static boolean isParameterTypeCompatible(Class<?> paramType, Class<?> methodParamType) {
        if (paramType.equals(methodParamType)) {
            return true;
        }
        
        if (paramType.isPrimitive()) {
            if (methodParamType.isPrimitive()) {
                return paramType.equals(methodParamType);
            } else {
                return (paramType == float.class && methodParamType == Float.class) ||
                       (paramType == int.class && methodParamType == Integer.class) ||
                       (paramType == double.class && methodParamType == Double.class) ||
                       (paramType == boolean.class && methodParamType == Boolean.class) ||
                       (paramType == long.class && methodParamType == Long.class) ||
                       (paramType == short.class && methodParamType == Short.class) ||
                       (paramType == byte.class && methodParamType == Byte.class) ||
                       (paramType == char.class && methodParamType == Character.class);
            }
        } else {
            if (methodParamType.isPrimitive()) {
                return (paramType == Float.class && methodParamType == float.class) ||
                       (paramType == Integer.class && methodParamType == int.class) ||
                       (paramType == Double.class && methodParamType == double.class) ||
                       (paramType == Boolean.class && methodParamType == boolean.class) ||
                       (paramType == Long.class && methodParamType == long.class) ||
                       (paramType == Short.class && methodParamType == short.class) ||
                       (paramType == Byte.class && methodParamType == byte.class) ||
                       (paramType == Character.class && methodParamType == char.class);
            } else {
                return methodParamType.isAssignableFrom(paramType);
            }
        }
    }

    public static Field getObfuscatedField(Class<?> klass, String[] fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                Field field = klass.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                continue;
            }
        }
        return null;
    }

    public static Object getObfuscatedFieldValue(Class<?> klass, String[] fieldNames, Object instance) {
        try {
            Field field = getObfuscatedField(klass, fieldNames);
            if (field != null) {
                field.setAccessible(true);
                return field.get(instance);
            }
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void setObfuscatedFieldValue(Class<?> klass, String[] fieldNames, Object instance, Object value) {
        try {
            Field field = getObfuscatedField(klass, fieldNames);
            if (field != null) {
                field.setAccessible(true);
                field.set(instance, value);
            }
        } catch (IllegalAccessException e) {
        }
    }

    public static Object invokeObfuscatedMethod(Class<?> klass, String[] methodNames, Object instance, Object... args) {
        try {
            Class<?>[] paramTypes = getParameterTypes(args);
            Method method = getObfuscatedMethod(klass, methodNames, paramTypes);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(instance, args);
            } else {
                logger.debug("Available methods in class {}: {}", klass.getName(), Arrays.toString(klass.getDeclaredMethods()));
            }
        } catch (Exception e) {
            logger.error("Error invoking obfuscated method:", e);
        }
        return null;
    }

    private static Class<?>[] getParameterTypes(Object[] args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Float) {
                parameterTypes[i] = float.class;
            } else if (args[i] instanceof Integer) {
                parameterTypes[i] = int.class;
            } else if (args[i] instanceof Double) {
                parameterTypes[i] = double.class;
            } else if (args[i] instanceof Boolean) {
                parameterTypes[i] = boolean.class;
            } else if (args[i] instanceof Long) {
                parameterTypes[i] = long.class;
            } else if (args[i] instanceof Short) {
                parameterTypes[i] = short.class;
            } else if (args[i] instanceof Byte) {
                parameterTypes[i] = byte.class;
            } else if (args[i] instanceof Character) {
                parameterTypes[i] = char.class;
            } else {
                parameterTypes[i] = args[i].getClass();
            }
        }
        return parameterTypes;
    }
}

