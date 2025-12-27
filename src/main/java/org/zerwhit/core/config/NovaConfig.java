package org.zerwhit.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.module.ToggleMode;
import org.zerwhit.core.util.SafeLogger;
import javafx.scene.input.KeyCode;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public final class NovaConfig {
    
    private static final class ConfigConstants {
        private static final String CONFIG_DIR_NAME = ConfigurationConstants.FileSystem.CONFIG_DIRECTORY_NAME;
        private static final String CONFIG_FILE_NAME = ConfigurationConstants.FileSystem.CONFIG_FILE_NAME;
        private static final String MODULE_ENABLED_KEY = ConfigurationConstants.JsonKeys.MODULE_ENABLED;
        private static final String MODULE_KEY_BINDING_KEY = ConfigurationConstants.JsonKeys.MODULE_KEY_BINDING;
        private static final String MODULE_TOGGLE_MODE_KEY = ConfigurationConstants.JsonKeys.MODULE_TOGGLE_MODE;
        private static final String MODULE_CONFIGS_KEY = ConfigurationConstants.JsonKeys.MODULE_CONFIGS;
        
        private ConfigConstants() {
            // Private constructor to prevent instantiation
        }
    }
    
    private static final SafeLogger LOGGER = SafeLogger.getLogger(NovaConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile NovaConfig instance;
    
    private final File configFile;
    private JsonObject configData;
    
    private NovaConfig() {
        File minecraftDir = Minecraft.getMinecraft().mcDataDir;
        File configDir = new File(minecraftDir, ConfigConstants.CONFIG_DIR_NAME);
        
        initializeConfigDirectory(configDir);
        this.configFile = new File(configDir, ConfigConstants.CONFIG_FILE_NAME);
        this.configData = new JsonObject();
        
        loadConfig();
    }
    
    public static NovaConfig getInstance() {
        if (instance == null) {
            synchronized (NovaConfig.class) {
                if (instance == null) {
                    instance = new NovaConfig();
                }
            }
        }
        return instance;
    }
    
    private void initializeConfigDirectory(File configDir) {
        if (!configDir.exists() && !configDir.mkdirs()) {
            LOGGER.error("Failed to create configuration directory: {}", configDir.getAbsolutePath());
        }
    }
    
    public void loadConfig() {
        if (!configFile.exists()) {
            LOGGER.info("Configuration file not found, creating new one");
            saveConfig();
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            JsonParser parser = new JsonParser();
            configData = parser.parse(reader).getAsJsonObject();
            LOGGER.info("Configuration loaded successfully");
        } catch (FileNotFoundException e) {
            LOGGER.error("Configuration file not found: {}", configFile.getAbsolutePath());
            configData = new JsonObject();
        } catch (IOException e) {
            LOGGER.error("I/O error while loading configuration: {}", e.getMessage());
            configData = new JsonObject();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration: {}", e.getMessage(), e);
            configData = new JsonObject();
        }
    }
    
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(configData, writer);
            LOGGER.info("Configuration saved successfully");
        } catch (IOException e) {
            LOGGER.error("I/O error while saving configuration: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to save configuration: {}", e.getMessage(), e);
        }
    }
    
    public void saveModuleConfig(ModuleBase module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        
        LOGGER.info("Saving configuration for module: {}", module.name);
        LOGGER.debug("Module configuration data: {}", module.config);
        
        JsonObject moduleConfig = createModuleConfigJson(module);
        configData.add(module.name, moduleConfig);
        saveConfig();
    }
    
    private JsonObject createModuleConfigJson(ModuleBase module) {
        JsonObject moduleConfig = new JsonObject();
        moduleConfig.addProperty(ConfigConstants.MODULE_ENABLED_KEY, module.enabled);
        
        if (module.bindingKey != null) {
            moduleConfig.addProperty(ConfigConstants.MODULE_KEY_BINDING_KEY, module.bindingKey.getName());
        }
        
        if (module.toggleMode != null) {
            moduleConfig.addProperty(ConfigConstants.MODULE_TOGGLE_MODE_KEY, module.toggleMode.name());
        }
        
        JsonObject configs = createModuleSettingsJson(module);
        moduleConfig.add(ConfigConstants.MODULE_CONFIGS_KEY, configs);
        
        return moduleConfig;
    }
    
    private JsonObject createModuleSettingsJson(ModuleBase module) {
        JsonObject configs = new JsonObject();
        
        for (Map.Entry<String, Object> entry : module.config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            addConfigProperty(configs, key, value);
        }
        
        return configs;
    }
    
    private void addConfigProperty(JsonObject configs, String key, Object value) {
        if (value instanceof String) {
            configs.addProperty(key, (String) value);
        } else if (value instanceof Number) {
            configs.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            configs.addProperty(key, (Boolean) value);
        } else {
            LOGGER.warn("Unsupported configuration type for key '{}': {}", key, value.getClass().getSimpleName());
        }
    }
    
    public void loadModuleConfig(ModuleBase module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        
        LOGGER.info("Loading configuration for module: {}", module.name);
        LOGGER.debug("Available configurations: {}", configData.entrySet());
        
        if (!configData.has(module.name)) {
            LOGGER.info("No saved configuration found for module: {}", module.name);
            return;
        }
        
        try {
            JsonObject moduleConfig = configData.getAsJsonObject(module.name);
            LOGGER.debug("Found configuration for module {}: {}", module.name, moduleConfig);
            
            loadModuleState(module, moduleConfig);
            loadModuleKeyBinding(module, moduleConfig);
            loadModuleToggleMode(module, moduleConfig);
            loadModuleSettings(module, moduleConfig);
            
            LOGGER.info("Configuration loaded for module: {}", module.name);
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration for module {}: {}", module.name, e.getMessage(), e);
        }
    }
    
    private void loadModuleState(ModuleBase module, JsonObject moduleConfig) {
        if (moduleConfig.has(ConfigConstants.MODULE_ENABLED_KEY)) {
            boolean enabled = moduleConfig.get(ConfigConstants.MODULE_ENABLED_KEY).getAsBoolean();
            updateModuleState(module, enabled);
        }
    }
    
    private void updateModuleState(ModuleBase module, boolean enabled) {
        if (enabled != module.enabled) {
            if (enabled) {
                module.enabled = true;
                module.onEnable();
            } else {
                module.enabled = false;
                module.onDisable();
            }
        }
    }
    
    private void loadModuleKeyBinding(ModuleBase module, JsonObject moduleConfig) {
        if (moduleConfig.has(ConfigConstants.MODULE_KEY_BINDING_KEY)) {
            String keyBindingName = moduleConfig.get(ConfigConstants.MODULE_KEY_BINDING_KEY).getAsString();
            try {
                KeyCode keyCode = KeyCode.valueOf(keyBindingName);
                module.setBindingKey(keyCode);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid key binding '{}' for module {}", keyBindingName, module.name);
            }
        }
    }
    
    private void loadModuleToggleMode(ModuleBase module, JsonObject moduleConfig) {
        if (moduleConfig.has(ConfigConstants.MODULE_TOGGLE_MODE_KEY)) {
            String toggleModeName = moduleConfig.get(ConfigConstants.MODULE_TOGGLE_MODE_KEY).getAsString();
            try {
                ToggleMode mode = ToggleMode.valueOf(toggleModeName);
                module.setToggleMode(mode);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid toggle mode '{}' for module {}", toggleModeName, module.name);
            }
        }
    }
    
    private void loadModuleSettings(ModuleBase module, JsonObject moduleConfig) {
        if (moduleConfig.has(ConfigConstants.MODULE_CONFIGS_KEY)) {
            JsonObject configs = moduleConfig.getAsJsonObject(ConfigConstants.MODULE_CONFIGS_KEY);
            
            for (Map.Entry<String, com.google.gson.JsonElement> entry : configs.entrySet()) {
                String key = entry.getKey();
                try {
                    Object newValue = parseConfigValue(configs, key, module);
                    if (newValue != null) {
                        module.config.put(key, newValue);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load configuration '{}' for module {}: {}", key, module.name, e.getMessage());
                }
            }
        }
    }
    
    private Object parseConfigValue(JsonObject configs, String key, ModuleBase module) {
        if (!configs.get(key).isJsonPrimitive()) {
            return null;
        }
        
        com.google.gson.JsonPrimitive primitive = configs.get(key).getAsJsonPrimitive();
        
        if (primitive.isString()) {
            return primitive.getAsString();
        } else if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            return parseNumericValue(primitive, key, module);
        }
        
        return null;
    }
    
    private Object parseNumericValue(com.google.gson.JsonPrimitive primitive, String key, ModuleBase module) {
        if (module.configTypes.containsKey(key)) {
            Class<?> type = module.configTypes.get(key);
            
            if (type == Integer.class || type == int.class) {
                return primitive.getAsInt();
            } else if (type == Double.class || type == double.class) {
                return primitive.getAsDouble();
            } else if (type == Float.class || type == float.class) {
                return primitive.getAsFloat();
            } else if (type == Long.class || type == long.class) {
                return primitive.getAsLong();
            }
        }
        
        if (module.config.containsKey(key)) {
            Object currentValue = module.config.get(key);
            
            if (currentValue instanceof Integer) {
                return primitive.getAsInt();
            } else if (currentValue instanceof Double) {
                return primitive.getAsDouble();
            } else if (currentValue instanceof Float) {
                return primitive.getAsFloat();
            } else if (currentValue instanceof Long) {
                return primitive.getAsLong();
            }
        }
        
        return primitive.getAsDouble();
    }
    
    public void saveAllModules() {
        for (java.util.List<ModuleBase> modules : ModuleBase.categories.values()) {
            for (ModuleBase module : modules) {
                saveModuleConfig(module);
            }
        }
    }
    
    public void loadAllModules() {
        for (java.util.List<ModuleBase> modules : ModuleBase.categories.values()) {
            for (ModuleBase module : modules) {
                loadModuleConfig(module);
            }
        }
    }
    
    public void resetModuleConfig(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        
        configData.remove(moduleName);
        saveConfig();
        LOGGER.info("Configuration reset for module: {}", moduleName);
    }

    public void resetAllConfigs() {
        configData = new JsonObject();
        saveConfig();
        LOGGER.info("All configurations have been reset");
    }
    
    public Map<String, Object> getModuleConfig(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        
        Map<String, Object> result = new HashMap<>();
        
        if (configData.has(moduleName)) {
            JsonObject moduleConfig = configData.getAsJsonObject(moduleName);
            
            extractModuleConfiguration(result, moduleConfig);
        }
        
        return result;
    }
    
    private void extractModuleConfiguration(Map<String, Object> result, JsonObject moduleConfig) {
        if (moduleConfig.has(ConfigConstants.MODULE_ENABLED_KEY)) {
            result.put(ConfigConstants.MODULE_ENABLED_KEY, moduleConfig.get(ConfigConstants.MODULE_ENABLED_KEY).getAsBoolean());
        }
        
        if (moduleConfig.has(ConfigConstants.MODULE_CONFIGS_KEY)) {
            JsonObject configs = moduleConfig.getAsJsonObject(ConfigConstants.MODULE_CONFIGS_KEY);
            
            for (Map.Entry<String, com.google.gson.JsonElement> entry : configs.entrySet()) {
                result.put(entry.getKey(), configs.get(entry.getKey()));
            }
        }
    }
}