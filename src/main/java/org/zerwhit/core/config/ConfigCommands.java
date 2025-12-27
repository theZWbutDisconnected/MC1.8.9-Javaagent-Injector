package org.zerwhit.core.config;

import org.zerwhit.core.util.SafeLogger;

public final class ConfigCommands {
    
    private static final SafeLogger LOGGER = SafeLogger.getLogger(ConfigCommands.class);
    
    private ConfigCommands() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    public static void saveAllConfigs() {
        try {
            ConfigManager.getInstance().saveAllConfigurations();
            LOGGER.info("All configurations saved successfully via command");
        } catch (ConfigManager.ConfigurationException e) {
            handleCommandFailure("save configurations", e);
        } catch (Exception e) {
            handleUnexpectedCommandFailure("save configurations", e);
        }
    }
    
    public static void reloadAllConfigs() {
        try {
            ConfigManager.getInstance().reloadConfigurations();
            LOGGER.info("All configurations reloaded successfully via command");
        } catch (ConfigManager.ConfigurationException e) {
            handleCommandFailure("reload configurations", e);
        } catch (Exception e) {
            handleUnexpectedCommandFailure("reload configurations", e);
        }
    }
    
    public static void resetModuleConfig(String moduleName) {
        validateModuleName(moduleName);
        
        try {
            ConfigManager.getInstance().resetModuleConfiguration(moduleName);
            LOGGER.info("Configuration reset for module '{}' via command", moduleName);
        } catch (ConfigManager.ConfigurationException e) {
            handleCommandFailure(String.format("reset configuration for module '%s'", moduleName), e);
        } catch (Exception e) {
            handleUnexpectedCommandFailure(String.format("reset configuration for module '%s'", moduleName), e);
        }
    }
    
    public static void resetAllConfigs() {
        try {
            ConfigManager.getInstance().resetAllConfigurations();
            LOGGER.info("All configurations reset to defaults via command");
        } catch (ConfigManager.ConfigurationException e) {
            handleCommandFailure("reset all configurations", e);
        } catch (Exception e) {
            handleUnexpectedCommandFailure("reset all configurations", e);
        }
    }
    
    public static String getConfigPath() {
        try {
            return ConfigManager.getInstance().getConfigDirectory();
        } catch (Exception e) {
            LOGGER.error("Failed to get configuration path", e);
            return "Unknown";
        }
    }
    
    public static boolean configFileExists() {
        try {
            return ConfigManager.getInstance().isConfigFileExists();
        } catch (Exception e) {
            LOGGER.error("Failed to check configuration file existence", e);
            return false;
        }
    }
    
    private static void validateModuleName(String moduleName) {
        if (moduleName == null || moduleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
    }
    
    private static void handleCommandFailure(String operation, ConfigManager.ConfigurationException cause) {
        String errorMessage = String.format("Failed to %s", operation);
        LOGGER.error(errorMessage, cause);
        throw new ConfigurationCommandException(errorMessage, cause);
    }
    
    private static void handleUnexpectedCommandFailure(String operation, Exception cause) {
        String errorMessage = String.format("Unexpected error while attempting to %s", operation);
        LOGGER.error(errorMessage, cause);
        throw new ConfigurationCommandException(errorMessage, cause);
    }
    
    public static class ConfigurationCommandException extends RuntimeException {
        
        public ConfigurationCommandException(String message) {
            super(message);
        }
        
        public ConfigurationCommandException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}