package com.zerwhit.core;

import java.util.HashMap;
import java.util.Map;

public class HookConfig {
    
    public enum HookType {
        BEFORE,
        AFTER,
        REPLACE,
        CONDITIONAL
    }
    
    public static class HookEntry {
        public final String className;
        public final String methodName;
        public final String methodDesc;
        public final HookType hookType;
        public final String hookMethod;
        public final String[] parameters;
        public final int[] parameterIndices;
        
        public HookEntry(String className, String methodName, String methodDesc, 
                        HookType hookType, String hookMethod, String[] parameters) {
            this(className, methodName, methodDesc, hookType, hookMethod, parameters, null);
        }
        
        public HookEntry(String className, String methodName, String methodDesc, 
                        HookType hookType, String hookMethod, String[] parameters, int[] parameterIndices) {
            this.className = className;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.hookType = hookType;
            this.hookMethod = hookMethod;
            this.parameters = parameters;
            this.parameterIndices = parameterIndices;
        }
        
        public String getHookMethodDescriptor() {
            if (parameters == null || parameters.length == 0) {
                return "()V";
            }
            
            StringBuilder desc = new StringBuilder("(");
            for (String param : parameters) {
                desc.append(param);
            }
            desc.append(")V");
            return desc.toString();
        }
        
        public int getParameterIndex(int paramIndex) {
            if (parameterIndices != null && paramIndex < parameterIndices.length) {
                return parameterIndices[paramIndex];
            }
            return paramIndex + 1;
        }
    }
    
    private static final Map<String, HookEntry> HOOK_CONFIGS = new HashMap<>();
    
    static {
        initializeHookConfigs();
    }
    
    private static void initializeHookConfigs() {
        addHookConfig("net/minecraft/client/Minecraft", new String[]{"runGameLoop", "func_71411_J"}, "()V", 
                     HookType.BEFORE, "onGameLoop", null);
        
        addHookConfig("net/minecraft/client/Minecraft", new String[]{"runTick", "func_71407_l"}, "()V", 
                     HookType.BEFORE, "onPreTick", null);
        addHookConfig("net/minecraft/client/Minecraft", new String[]{"runTick", "func_71407_l"}, "()V", 
                     HookType.AFTER, "onPostTick", null);

        addHookConfig("net/minecraft/client/Minecraft", new String[]{"updateDisplay", "func_175601_h"}, "()V", 
                     HookType.BEFORE, "onUpdateDisplay", null);
        
        addHookConfig("net/minecraft/entity/player/EntityPlayer", 
                     new String[]{"func_70071_h_", "onUpdate"}, "()V", 
                     HookType.BEFORE, "onPlayerPreUpdate", null);
        addHookConfig("net/minecraft/entity/player/EntityPlayer", 
                     new String[]{"func_70071_h_", "onUpdate"}, "()V", 
                     HookType.AFTER, "onPlayerPostUpdate", null);
        
        addHookConfig("net/minecraft/entity/player/EntityPlayer", 
                     new String[]{"func_70097_a", "attackEntityFrom"}, 
                     "(Lnet/minecraft/util/DamageSource;F)Z", 
                     HookType.BEFORE, "onPlayerHurt", null);
        
        addHookConfig("net/minecraft/client/renderer/ItemRenderer", 
                     new String[]{"func_78440_a", "renderItemInFirstPerson"}, 
                     "(F)V", 
                     HookType.REPLACE, "renderItemInFirstPersonHook", new String[]{"F"});
        
        addHookConfig("net/minecraft/client/renderer/EntityRenderer", 
                     new String[]{"func_78467_g", "orientCamera"}, 
                     "(F)V", 
                     HookType.REPLACE, "orientCameraHook", new String[]{"F"});
        
        addHookConfig("net/minecraft/client/renderer/EntityRenderer",
                     new String[]{"func_181560_a", "updateCameraAndRender"},
                     "(FJ)V",
                     HookType.REPLACE, "updateCameraAndRenderHook", new String[]{"FJ"});
    }
    
    private static void addHookConfig(String className, String[] methodNames, String methodDesc,
                                     HookType hookType, String hookMethod, String[] parameters) {
        for (String methodName : methodNames) {
            addHookConfig(className, methodName, methodDesc, hookType, hookMethod, parameters);
        }
    }
    
    private static void addHookConfig(String className, String methodName, String methodDesc,
                                     HookType hookType, String hookMethod, String[] parameters) {
        String key = generateKey(className, methodName, methodDesc);
        HOOK_CONFIGS.put(key, new HookEntry(className, methodName, methodDesc, hookType, hookMethod, parameters));
    }
    
    private static String generateKey(String className, String methodName, String methodDesc) {
        return className + "#" + methodName + "#" + methodDesc;
    }
    
    public static HookEntry getHookConfig(String className, String methodName, String methodDesc) {
        String key = generateKey(className, methodName, methodDesc);
        return HOOK_CONFIGS.get(key);
    }
    
    public static boolean hasHookConfig(String className, String methodName, String methodDesc) {
        return getHookConfig(className, methodName, methodDesc) != null;
    }
    
    public static Map<String, HookEntry> getAllHookConfigs() {
        return new HashMap<>(HOOK_CONFIGS);
    }
}