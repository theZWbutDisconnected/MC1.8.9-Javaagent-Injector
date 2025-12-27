package org.zerwhit.core.config;

import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.SafeLogger;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.Map;

public final class ConfigManager {
    
    private static final class ConfigManagerConstants {
        private static final String CONFIG_DIRECTORY_NAME = "Nova";
        private static final String CONFIG_FILE_NAME = "configs.json";
        
        private ConfigManagerConstants() {
            // Private constructor to prevent instantiation
        }
    }
    
    private static final SafeLogger LOGGER = SafeLogger.getLogger(ConfigManager.class);
    private static volatile ConfigManager instance;
    
    private ConfigManager() {
        // Private constructor to prevent instantiation
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
    
    public void initialize() {
        try {
            LOGGER.info("Initializing Nova configuration system");
            ModuleBase.loadAllConfigurations();
            LOGGER.info("Configuration system initialized successfully");
        } catch (Exception e) {
            String errorMessage = "Failed to initialize configuration system";
            LOGGER.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e);
        }
    }
    
    public void saveAllConfigurations() {
        try {
            LOGGER.info("Saving all module configurations");
            NovaConfig.getInstance().saveAllModules();
            LOGGER.info("All configurations saved successfully");
        } catch (Exception e) {
            String errorMessage = "Failed to save configurations";
            LOGGER.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e);
        }
    }
    
    public void reloadConfigurations() {
        try {
            LOGGER.info("Reloading all configurations");
            NovaConfig.getInstance().loadAllModules();
            LOGGER.info("All configurations reloaded successfully");
        } catch (Exception e) {
            String errorMessage = "Failed to reload configurations";
            LOGGER.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e);
        }
    }
    
    public void resetModuleConfiguration(String moduleName) {
        validateModuleName(moduleName);
        
        try {
            LOGGER.info("Resetting configuration for module: {}", moduleName);
            NovaConfig.getInstance().resetModuleConfig(moduleName);
            LOGGER.info("Configuration reset for module: {} completed successfully", moduleName);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to reset configuration for module: %s", moduleName);
            LOGGER.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e);
        }
    }
    
    public void resetAllConfigurations() {
        try {
            LOGGER.info("Resetting all configurations to defaults");
            NovaConfig.getInstance().resetAllConfigs();
            NovaConfig.getInstance().loadAllModules();
            LOGGER.info("All configurations reset successfully");
        } catch (Exception e) {
            String errorMessage = "Failed to reset all configurations";
            LOGGER.error(errorMessage, e);
            throw new ConfigurationException(errorMessage, e);
        }
    }
    
    public Map<String, Object> getModuleConfiguration(String moduleName) {
        validateModuleName(moduleName);
        return NovaConfig.getInstance().getModuleConfig(moduleName);
    }
    
    public String getConfigDirectory() {
        File minecraftDir = Minecraft.getMinecraft().mcDataDir;
        File configDir = new File(minecraftDir, ConfigManagerConstants.CONFIG_DIRECTORY_NAME);
        return configDir.getAbsolutePath();
    }
    
    public boolean isConfigFileExists() {
        try {
            File minecraftDir = Minecraft.getMinecraft().mcDataDir;
            File configFile = new File(new File(minecraftDir, ConfigManagerConstants.CONFIG_DIRECTORY_NAME), 
                                       ConfigManagerConstants.CONFIG_FILE_NAME);
            return configFile.exists() && configFile.isFile();
        } catch (Exception e) {
            LOGGER.error("Error checking configuration file existence", e);
            return false;
        }
    }
    
    public void exportConfiguration(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        
        String message = "Export configuration functionality not yet implemented";
        LOGGER.warn(message);
        throw new UnsupportedOperationException(message);
    }
    
    public void importConfiguration(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        String message = "Import configuration functionality not yet implemented";
        LOGGER.warn(message);
        throw new UnsupportedOperationException(message);
    }
    
    private void validateModuleName(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
    }
    
    public static class ConfigurationException extends RuntimeException {
        
        public ConfigurationException(String message) {
            super(message);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}