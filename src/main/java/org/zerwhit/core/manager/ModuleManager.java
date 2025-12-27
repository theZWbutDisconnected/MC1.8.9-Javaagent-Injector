package org.zerwhit.core.manager;

import org.zerwhit.core.module.IEventModule;
import org.zerwhit.core.module.ITickableModule;
import org.zerwhit.core.module.IRenderModule;
import org.zerwhit.core.module.IVisualModule;
import org.zerwhit.core.module.ModuleBase;
import net.minecraft.client.Minecraft;
import org.zerwhit.core.util.SafeLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ModuleManager {
    private static final SafeLogger logger = SafeLogger.getLogger(ModuleManager.class);
    
    private final Map<ModuleCategory, List<ModuleBase>> modulesByCategory = new ConcurrentHashMap<>();
    private final Map<ModuleHookType, List<ModuleBase>> modulesByHookType = new ConcurrentHashMap<>();
    private final Map<ModuleBase, ModulePriority> modulePriorities = new ConcurrentHashMap<>();
    private final Map<ModuleBase, ModuleState> moduleStates = new HashMap<>();
    private final List<ModuleLifecycleListener> lifecycleListeners = new ArrayList<>();
    
    private boolean initialized = false;
    
    public ModuleManager() {
        initializeMaps();
    }
    
    private void initializeMaps() {
        for (ModuleCategory category : ModuleCategory.values()) {
            modulesByCategory.put(category, new ArrayList<>());
        }
        for (ModuleHookType hookType : ModuleHookType.values()) {
            modulesByHookType.put(hookType, new ArrayList<>());
        }
    }
    
    public void initialize() {
        if (initialized) {
            return;
        }
        
        registerAllModules();
        initialized = true;
    }
    
    private void registerAllModules() {
        for (Map.Entry<String, List<ModuleBase>> entry : ModuleBase.categories.entrySet()) {
            ModuleCategory category = ModuleCategory.fromString(entry.getKey());
            if (category != null) {
                registerModulesForCategory(category, entry.getValue());
            }
        }
    }
    
    private void registerModulesForCategory(ModuleCategory category, List<ModuleBase> modules) {
        modulesByCategory.get(category).addAll(modules);
        for (ModuleBase module : modules) {
            registerModuleByHookType(module);
            setModulePriority(module);
            setModuleState(module, ModuleState.DISABLED);
        }
    }
    
    private void setModulePriority(ModuleBase module) {
        ModulePriority priority = determineModulePriority(module);
        modulePriorities.put(module, priority);
    }
    
    private ModulePriority determineModulePriority(ModuleBase module) {
        if (isHighPriorityModule(module)) {
            return ModulePriority.HIGHEST;
        }
        
        ModuleCategory category = ModuleCategory.fromString(module.category);
        if (category != null) {
            return getPriorityForCategory(category);
        }
        
        return ModulePriority.NORMAL;
    }
    
    private boolean isHighPriorityModule(ModuleBase module) {
        return module.name.equals("KillAura") || module.name.equals("Speed");
    }
    
    private ModulePriority getPriorityForCategory(ModuleCategory category) {
        switch (category) {
            case COMBAT:
                return ModulePriority.HIGH;
            case MOVEMENT:
                return ModulePriority.NORMAL;
            case RENDER:
                return ModulePriority.LOW;
            case VISUAL:
                return ModulePriority.LOWEST;
            default:
                return ModulePriority.NORMAL;
        }
    }
    
    private void setModuleState(ModuleBase module, ModuleState state) {
        ModuleState oldState = moduleStates.get(module);
        moduleStates.put(module, state);
        
        if (oldState != state) {
            notifyModuleStateChange(module, oldState, state);
        }
    }
    
    private void notifyModuleStateChange(ModuleBase module, ModuleState oldState, ModuleState newState) {
        for (ModuleLifecycleListener listener : lifecycleListeners) {
            notifyListener(listener, module, newState);
        }
    }
    
    private void notifyListener(ModuleLifecycleListener listener, ModuleBase module, ModuleState newState) {
        try {
            switch (newState) {
                case ENABLED:
                    listener.onModuleEnabled(module);
                    break;
                case DISABLED:
                    listener.onModuleDisabled(module);
                    break;
                case ERROR:
                    listener.onModuleError(module, new Exception("Module error occurred"));
                    break;
            }
        } catch (Exception e) {
            logger.error("Error notifying lifecycle listener: {}", e.getMessage());
        }
    }
    
    public void addLifecycleListener(ModuleLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }
    
    public void removeLifecycleListener(ModuleLifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }
    
    public ModuleState getModuleState(ModuleBase module) {
        return moduleStates.getOrDefault(module, ModuleState.DISABLED);
    }
    
    private void registerModuleByHookType(ModuleBase module) {
        if (module instanceof ITickableModule) {
            modulesByHookType.get(ModuleHookType.TICK).add(module);
        }
        if (module instanceof IRenderModule) {
            modulesByHookType.get(ModuleHookType.RENDER).add(module);
        }
        if (module instanceof IVisualModule) {
            modulesByHookType.get(ModuleHookType.VISUAL).add(module);
        }
        if (module instanceof IEventModule) {
            modulesByHookType.get(ModuleHookType.EVENT).add(module);
        }
    }
    
    public List<ModuleBase> getModulesByCategory(ModuleCategory category) {
        return Collections.unmodifiableList(modulesByCategory.get(category));
    }
    
    public List<ModuleBase> getModulesByCategorySorted(ModuleCategory category) {
        List<ModuleBase> modules = new ArrayList<>(modulesByCategory.get(category));
        modules.sort(this::compareModulesByPriority);
        return modules;
    }
    
    private int compareModulesByPriority(ModuleBase m1, ModuleBase m2) {
        ModulePriority p1 = modulePriorities.get(m1);
        ModulePriority p2 = modulePriorities.get(m2);
        return Integer.compare(p1.getValue(), p2.getValue());
    }
    
    public List<ModuleBase> getModulesByHookType(ModuleHookType hookType) {
        return Collections.unmodifiableList(modulesByHookType.get(hookType));
    }
    
    public List<ModuleBase> getModulesByHookTypeSorted(ModuleHookType hookType) {
        List<ModuleBase> modules = new ArrayList<>(modulesByHookType.get(hookType));
        modules.sort(this::compareModulesByPriority);
        return modules;
    }
    
    public List<ModuleBase> getEnabledModules() {
        List<ModuleBase> enabledModules = new ArrayList<>();
        for (List<ModuleBase> modules : modulesByCategory.values()) {
            for (ModuleBase module : modules) {
                if (module.enabled) {
                    enabledModules.add(module);
                }
            }
        }
        return enabledModules;
    }
    
    public void invokeHook(ModuleHookType hookType, String funcName, Object... args) {
        if (!initialized) {
            return;
        }
        
        List<ModuleBase> modules = getModulesByHookTypeSorted(hookType);
        for (ModuleBase module : modules) {
            if (shouldInvokeModule(module)) {
                invokeModuleHook(module, hookType, funcName, args);
            }
        }
    }
    
    private boolean shouldInvokeModule(ModuleBase module) {
        if (!module.enabled) {
            return false;
        }
        
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld != null && mc.thePlayer != null;
    }

    private void invokeModuleHook(ModuleBase module, ModuleHookType hookType, Object[] args) {
        invokeModuleHook(module, hookType, "", args);
    }

    private void invokeModuleHook(ModuleBase module, ModuleHookType hookType, String funcName, Object[] args) {
        try {
            setModuleState(module, ModuleState.ENABLED);
            executeModuleHook(module, hookType, funcName, args);
        } catch (Exception e) {
            handleModuleError(module, hookType, e);
        }
    }
    
    private void executeModuleHook(ModuleBase module, ModuleHookType hookType, String funcName, Object[] args) {
        switch (hookType) {
            case TICK:
                executeTickHook(module);
                break;
            case RENDER:
                executeRenderHook(module, args);
                break;
            case VISUAL:
                executeVisualHook(module, funcName, args);
                break;
            case EVENT:
                executeEventHook(module, args);
                break;
        }
    }
    
    private void executeTickHook(ModuleBase module) {
        ((ITickableModule) module).onModuleTick();
    }
    
    private void executeRenderHook(ModuleBase module, Object[] args) {
        if (args.length >= 3 && args[0] instanceof Float && args[1] instanceof Integer && args[2] instanceof Integer) {
            ((IRenderModule) module).onRender((Float) args[0], (Integer) args[1], (Integer) args[2]);
        }
    }
    
    private void executeVisualHook(ModuleBase module, String funcName, Object[] args) {
        if (args.length >= 1 && args[0] instanceof Float) {
            ((IVisualModule) module).onHook(funcName, (Float) args[0]);
        }
    }
    
    private void executeEventHook(ModuleBase module, Object[] args) {
        if (args.length >= 1 && args[0] instanceof String) {
            String eventType = (String) args[0];
            Object[] eventArgs = extractEventArgs(args);
            ((IEventModule) module).onEvent(eventType, eventArgs);
        }
    }
    
    private Object[] extractEventArgs(Object[] args) {
        if (args.length <= 1) {
            return new Object[0];
        }
        
        Object[] eventArgs = new Object[args.length - 1];
        System.arraycopy(args, 1, eventArgs, 0, eventArgs.length);
        return eventArgs;
    }
    
    private void handleModuleError(ModuleBase module, ModuleHookType hookType, Exception e) {
        logger.error("Error invoking {} hook for module {}: {}", hookType, module.name, e.getMessage());
        setModuleState(module, ModuleState.ERROR);
        logger.error("Error details:", e);
    }
    
    public void invokeCategory(ModuleCategory category, ModuleHookType hookType, Object... args) {
        if (!initialized) {
            return;
        }
        
        List<ModuleBase> modules = getModulesByCategorySorted(category);
        for (ModuleBase module : modules) {
            if (shouldInvokeModule(module) && hasHookType(module, hookType)) {
                invokeModuleHook(module, hookType, args);
            }
        }
    }
    
    public void invokeModule(ModuleHookType hookType, Object... args) {
        if (!initialized) {
            return;
        }
        
        for (List<ModuleBase> modules : modulesByCategory.values()) {
            for (ModuleBase module : modules) {
                if (shouldInvokeModule(module) && hasHookType(module, hookType)) {
                    try {
                        invokeModuleHook(module, hookType, args);
                        return;
                    } catch (Exception e) {
                        logger.error("Error invoking module hook:", e);
                    }
                }
            }
        }
    }
    
    private boolean hasHookType(ModuleBase module, ModuleHookType hookType) {
        switch (hookType) {
            case TICK:
                return module instanceof ITickableModule;
            case RENDER:
                return module instanceof IRenderModule;
            case VISUAL:
                return module instanceof IVisualModule;
            case EVENT:
                return module instanceof IEventModule;
            default:
                return false;
        }
    }
    
    public int getTotalModuleCount() {
        int count = 0;
        for (List<ModuleBase> modules : modulesByCategory.values()) {
            count += modules.size();
        }
        return count;
    }
    
    public int getEnabledModuleCount() {
        return getEnabledModules().size();
    }
    
    public void cleanup() {
        for (ModuleBase module : moduleStates.keySet()) {
            setModuleState(module, ModuleState.DISABLED);
        }
        
        modulesByCategory.clear();
        modulesByHookType.clear();
        modulePriorities.clear();
        moduleStates.clear();
        lifecycleListeners.clear();
        initialized = false;
        logger.info("ModuleManager cleaned up");
    }
    
    public enum ModuleCategory {
        COMBAT("Combat", 0),
        MOVEMENT("Movement", 1),
        RENDER("Render", 2),
        VISUAL("Visual", 3);
        
        private final String name;
        private final int priority;
        
        ModuleCategory(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
        
        public String getName() {
            return name;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public static ModuleCategory fromString(String name) {
            if (name == null) {
                return null;
            }
            
            for (ModuleCategory category : values()) {
                if (category.name.equalsIgnoreCase(name)) {
                    return category;
                }
            }
            return null;
        }
    }
    
    public enum ModuleHookType {
        TICK,
        RENDER,
        VISUAL,
        EVENT
    }
    
    public enum ModulePriority {
        HIGHEST(5),
        HIGH(4),
        NORMAL(3),
        LOW(2),
        LOWEST(1);
        
        private final int value;
        
        ModulePriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    public enum ModuleState {
        DISABLED,
        ENABLED,
        ERROR
    }
    
    public interface ModuleLifecycleListener {
        void onModuleEnabled(ModuleBase module);
        void onModuleDisabled(ModuleBase module);
        void onModuleError(ModuleBase module, Exception error);
    }
}