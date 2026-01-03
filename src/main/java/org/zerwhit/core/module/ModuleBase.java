package org.zerwhit.core.module;

import javafx.scene.input.KeyCode;
import net.minecraft.network.Packet;
import org.zerwhit.core.Hooks;
import org.zerwhit.core.config.NovaConfig;
import org.zerwhit.core.config.ConfigInitialization;
import org.zerwhit.core.module.combat.ModuleAutoClicker;
import org.zerwhit.core.module.combat.ModuleRightClicker;
import org.zerwhit.core.module.combat.ModuleSlientAura;
import org.zerwhit.core.module.player.ModuleEagle;
import org.zerwhit.core.module.player.ModuleFastPlace;
import org.zerwhit.core.util.island.IslandNotification;
import org.zerwhit.core.module.visual.ModuleDynamicIsland;
import org.zerwhit.core.util.SafeLogger;
import org.zerwhit.core.module.movement.*;
import org.zerwhit.core.module.player.ModuleScaffold;
import org.zerwhit.core.module.render.ModuleArraylist;
import org.zerwhit.core.module.visual.ModuleFreeLook;
import org.zerwhit.core.module.visual.ModuleLegacyAnim;
import org.zerwhit.core.manager.RotationManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public abstract class ModuleBase {
    private static final SafeLogger logger = SafeLogger.getLogger(ModuleBase.class);

    public static final Map<String, List<ModuleBase>> categories = new HashMap<>();
    public static final Map<Integer, Set<ModuleBase>> keyBindings = new HashMap<>();
    public static final Set<Integer> pressedKeys = new HashSet<>();
    
    protected final Minecraft mc;
    protected final RotationManager rotMng;
    
    public String name;
    public boolean enabled;
    public String category;
    public KeyCode bindingKey;
    public ToggleMode toggleMode;
    
    public final Map<String, Object> config = new HashMap<>();
    public final Map<String, Class<?>> configTypes = new HashMap<>();
    public final Map<String, Number[]> rangedVals = new HashMap<>();

    public static ModuleScaffold scaffold;
    public static ModuleFreeLook frelook;
    
    static {
        initializeModules();
    }
    
    private static void initializeModules() {
        addModule(new ModuleSprint());
        addModule(scaffold = new ModuleScaffold());
        addModule(new ModuleEagle());
        addModule(new ModuleArraylist());
        addModule(new ModuleAutoClicker());
        addModule(new ModuleRightClicker());
        addModule(new ModuleFastPlace());
        addModule(new ModuleSlientAura());
        addModule(new ModuleLegacyAnim());
        addModule(frelook = new ModuleFreeLook(KeyCode.G));
        addModule(ModuleDynamicIsland.getInstance());
    }
    
    private static void addModule(ModuleBase module) {
        categories.computeIfAbsent(module.category, k -> new ArrayList<>()).add(module);
        
        if (module.bindingKey != null) {
            registerKeyBinding(module);
        }
    }
    
    private static void registerKeyBinding(ModuleBase module) {
        if (module.bindingKey == null) {
            return;
        }
        
        int keyCode = convertKeyCodeToLWJGL(module.bindingKey);
        if (keyCode != -1) {
            keyBindings.computeIfAbsent(keyCode, k -> new HashSet<>()).add(module);
            logger.info("Registered key binding for {}: {}", module.name, module.bindingKey.getName());
        }
    }
    
    private static void unregisterKeyBinding(ModuleBase module) {
        if (module.bindingKey == null) {
            return;
        }
        
        int keyCode = convertKeyCodeToLWJGL(module.bindingKey);
        if (keyCode != -1) {
            Set<ModuleBase> modules = keyBindings.get(keyCode);
            if (modules != null) {
                modules.remove(module);
                if (modules.isEmpty()) {
                    keyBindings.remove(keyCode);
                }
            }
        }
    }
    
    private static int convertKeyCodeToLWJGL(KeyCode keyCode) {
        try {
            String keyName = keyCode.getName().toUpperCase();
            return Keyboard.class.getField("KEY_" + keyName).getInt(null);
        } catch (Exception e) {
            logger.warn("Failed to convert key code {}: {}", keyCode.getName(), e.getMessage());
            return -1;
        }
    }
    
    protected ModuleBase(String name, boolean enabled, String category) {
        this(name, enabled, category, null, ToggleMode.TOGGLE);
    }
    
    protected ModuleBase(String name, boolean enabled, String category, KeyCode bindingKey) {
        this(name, enabled, category, bindingKey, ToggleMode.TOGGLE);
    }
    
    protected ModuleBase(String name, boolean enabled, String category, KeyCode bindingKey, ToggleMode toggleMode) {
        this.mc = Minecraft.getMinecraft();
        this.rotMng = Hooks.rotMng;
        this.name = name;
        this.enabled = enabled;
        this.category = category;
        this.bindingKey = bindingKey;
        this.toggleMode = toggleMode;

        if (ConfigInitialization.isInitialized()) {
            loadModuleConfiguration();
        }
        
        if (this.enabled) {
            onEnable();
        }
    }
    
    private void loadModuleConfiguration() {
        if (ConfigInitialization.isInitialized()) {
            NovaConfig.getInstance().loadModuleConfig(this);
        }
    }
    
    public String getDisplayName() {
        return name;
    }
    
    public int getCategoryOrder() {
        return getCategoryPriority(category);
    }
    
    private int getCategoryPriority(String category) {
        switch (category) {
            case "Combat":
                return 0;
            case "Movement":
                return 1;
            case "Player":
                return 2;
            case "Render":
                return 3;
            case "Visual":
                return 4;
            default:
                return 5;
        }
    }

    public void addConfig(String key, Object defaultValue) {
        config.put(key, defaultValue);
        configTypes.put(key, defaultValue.getClass());
    }

    public void addRangedConfig(String key, Number defaultValue, Number minValue, Number maxValue) {
        addConfig(key, defaultValue);
        rangedVals.put(key, new Number[]{minValue, maxValue});
    }
    
    public Object getConfig(String key) {
        return config.get(key);
    }
    
    public void setConfig(String key, Object value) {
        config.put(key, value);
        saveModuleConfiguration();
    }
    
    private void saveModuleConfiguration() {
        if (ConfigInitialization.isInitialized()) {
            NovaConfig.getInstance().saveModuleConfig(this);
        }
    }
    
    public void loadConfiguration() {
        loadModuleConfiguration();
    }
    
    public void onEnable() {
        saveModuleConfiguration();
        if (ModuleDynamicIsland.getInstance() == null) return;
        ModuleDynamicIsland.getInstance().addCustomNotification("Module State",
                "Module " + name + " has been enabled",
                IslandNotification.NotificationType.MODULE_ENABLED,
                2000,
                1
        );
    }

    public void onDisable() {
        saveModuleConfiguration();
        if (ModuleDynamicIsland.getInstance() == null) return;
        ModuleDynamicIsland.getInstance().addCustomNotification("Module State",
                "Module " + name + " has been disabled.",
                IslandNotification.NotificationType.MODULE_DISABLED,
                2000,
                1
        );
    }
    
    public static void loadAllConfigurations() {
        logger.info("Loading configurations for all modules");
        for (Map.Entry<String, List<ModuleBase>> categoryEntry : categories.entrySet()) {
            for (ModuleBase module : categoryEntry.getValue()) {
                logger.info("Loading config for module: {}", module.name);
                module.loadConfiguration();
            }
        }
    }
    
    public static void reloadAllConfigurations() {
        logger.info("Reloading configurations for all modules");
        for (Map.Entry<String, List<ModuleBase>> categoryEntry : categories.entrySet()) {
            for (ModuleBase module : categoryEntry.getValue()) {
                logger.info("Reloading config for module: {}", module.name);
                module.loadConfiguration();
            }
        }
    }

    protected void sendPacket(Packet packet) {
        mc.getNetHandler().addToSendQueue(packet);
    }
    
    public void toggle() {
        this.enabled = !this.enabled;
        
        if (this.enabled) {
            onEnable();
        } else {
            onDisable();
        }
        
        saveModuleConfiguration();
    }
    
    public static void updateKeyBindings() {
        if (keyBindings.isEmpty()) {
            return;
        }
        
        for (Map.Entry<Integer, Set<ModuleBase>> entry : keyBindings.entrySet()) {
            int keyCode = entry.getKey();
            boolean isPressed = Keyboard.isKeyDown(keyCode);
            boolean wasPressed = pressedKeys.contains(keyCode);
            
            handleKeyPress(entry.getValue(), keyCode, isPressed, wasPressed);
            updatePressedKeys(keyCode, isPressed, wasPressed);
        }
    }
    
    private static void handleKeyPress(Set<ModuleBase> modules, int keyCode, boolean isPressed, boolean wasPressed) {
        if (isPressed && !wasPressed) {
            handleKeyPressDown(modules);
        } else if (!isPressed && wasPressed) {
            handleKeyRelease(modules);
        } else if (isPressed && wasPressed) {
            handleKeyHold(modules);
        }
    }
    
    private static void handleKeyPressDown(Set<ModuleBase> modules) {
        for (ModuleBase module : modules) {
            if (module.toggleMode == ToggleMode.TOGGLE) {
                module.toggle();
            } else if (module.toggleMode == ToggleMode.HOLD) {
                enableModule(module);
            }
        }
    }
    
    private static void handleKeyRelease(Set<ModuleBase> modules) {
        for (ModuleBase module : modules) {
            if (module.toggleMode == ToggleMode.HOLD) {
                disableModule(module);
            }
        }
    }
    
    private static void handleKeyHold(Set<ModuleBase> modules) {
        for (ModuleBase module : modules) {
            if (module.toggleMode == ToggleMode.HOLD) {
                enableModule(module);
            }
        }
    }
    
    private static void enableModule(ModuleBase module) {
        if (!module.enabled) {
            module.enabled = true;
            module.onEnable();
        }
    }
    
    private static void disableModule(ModuleBase module) {
        if (module.enabled) {
            module.enabled = false;
            module.onDisable();
        }
    }
    
    private static void updatePressedKeys(int keyCode, boolean isPressed, boolean wasPressed) {
        if (isPressed && !wasPressed) {
            pressedKeys.add(keyCode);
        } else if (!isPressed && wasPressed) {
            pressedKeys.remove(keyCode);
        }
    }
    
    public void setBindingKey(KeyCode newKey) {
        if (this.bindingKey != null) {
            unregisterKeyBinding(this);
        }
        
        this.bindingKey = newKey;
        
        if (newKey != null) {
            registerKeyBinding(this);
        }
        
        saveModuleConfiguration();
    }
    
    public KeyCode getBindingKey() {
        return bindingKey;
    }
    
    public String getBindingKeyName() {
        return bindingKey != null ? bindingKey.getName() : "None";
    }
    
    public ToggleMode getToggleMode() {
        return toggleMode;
    }
    
    public void setToggleMode(ToggleMode mode) {
        this.toggleMode = mode;
        saveModuleConfiguration();
    }
    
    public String getToggleModeName() {
        return toggleMode.name();
    }
    
    public void cycleStringConfig(String key) {
        Object currentValue = getConfig(key);
        if (!(currentValue instanceof String)) {
            return;
        }
        
        String currentStr = (String) currentValue;
        String newValue = cycleStringValue(currentStr);
        
        if (newValue != null) {
            setConfig(key, newValue);
        } else {
            logger.warn("No cycling logic implemented for string config '{}' with value '{}'", key, currentStr);
        }
    }
    
    private String cycleStringValue(String currentValue) {
        if (isBooleanString(currentValue)) {
            return String.valueOf(!Boolean.parseBoolean(currentValue));
        }
        
        if (isOnOffString(currentValue)) {
            return currentValue.equalsIgnoreCase("on") ? "off" : "on";
        }
        
        if (isEnableDisableString(currentValue)) {
            return currentValue.equalsIgnoreCase("enable") ? "disable" : "enable";
        }
        
        return null;
    }
    
    private boolean isBooleanString(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }
    
    private boolean isOnOffString(String value) {
        return value.equalsIgnoreCase("on") || value.equalsIgnoreCase("off");
    }
    
    private boolean isEnableDisableString(String value) {
        return value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("disable");
    }
    
    public double getMaxValueForConfig(String key) {
        return rangedVals.getOrDefault(key, new Number[]{0.0, 10.0})[1].doubleValue();
    }
    
    public double getMinValueForConfig(String key) {
        return rangedVals.getOrDefault(key, new Number[]{0.0, 10.0})[0].doubleValue();
    }
    
    public static Map<Integer, Set<ModuleBase>> getKeyBindings() {
        return new HashMap<>(keyBindings);
    }
    
    public static Set<ModuleBase> getModulesForKey(int keyCode) {
        return keyBindings.getOrDefault(keyCode, new HashSet<>());
    }

    public boolean isEnabled() {
        return enabled;
    }
}