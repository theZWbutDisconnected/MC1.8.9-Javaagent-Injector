package org.zerwhit.core.config;

public final class ConfigurationConstants {
    
    public static final class FileSystem {
        public static final String CONFIG_DIRECTORY_NAME = "Nova";
        public static final String CONFIG_FILE_NAME = "configs.json";
        public static final String LEGACY_CONFIG_FILE_NAME = "configs.json";
        
        private FileSystem() {
            // Private constructor to prevent instantiation
        }
    }
    
    public static final class JsonKeys {
        public static final String MODULE_ENABLED = "enabled";
        public static final String MODULE_KEY_BINDING = "keyBinding";
        public static final String MODULE_TOGGLE_MODE = "toggleMode";
        public static final String MODULE_CONFIGS = "configs";
        
        private JsonKeys() {
            // Private constructor to prevent instantiation
        }
    }

    public static final class Messages {
        public static final String CONFIG_LOADED_SUCCESS = "Configuration loaded successfully";
        public static final String CONFIG_SAVED_SUCCESS = "Configuration saved successfully";
        public static final String CONFIG_NOT_FOUND = "Configuration file not found, creating new one";
        public static final String MODULE_CONFIG_SAVED = "Configuration saved for module: {}";
        public static final String MODULE_CONFIG_LOADED = "Configuration loaded for module: {}";
        public static final String MODULE_CONFIG_RESET = "Configuration reset for module: {}";
        public static final String ALL_CONFIGS_RESET = "All configurations have been reset";
        
        private Messages() {
            // Private constructor to prevent instantiation
        }
    }
    
    public static final class ErrorMessages {
        public static final String CONFIG_LOAD_FAILED = "Failed to load configuration";
        public static final String CONFIG_SAVE_FAILED = "Failed to save configuration";
        public static final String MODULE_CONFIG_LOAD_FAILED = "Failed to load configuration for module";
        public static final String MODULE_CONFIG_SAVE_FAILED = "Failed to save configuration for module";
        public static final String INVALID_KEY_BINDING = "Invalid key binding '{}' for module {}";
        public static final String INVALID_TOGGLE_MODE = "Invalid toggle mode '{}' for module {}";
        public static final String UNSUPPORTED_CONFIG_TYPE = "Unsupported configuration type for key '{}'";
        public static final String NULL_MODULE = "Module cannot be null";
        public static final String EMPTY_MODULE_NAME = "Module name cannot be null or empty";
        public static final String NULL_FILE_NAME = "File name cannot be null or empty";
        public static final String NULL_FILE_PATH = "File path cannot be null or empty";
        
        private ErrorMessages() {
            // Private constructor to prevent instantiation
        }
    }
    
    public static final class Logging {
        public static final String INITIALIZING_CONFIG_SYSTEM = "Initializing Nova configuration system";
        public static final String CONFIG_SYSTEM_INITIALIZED = "Configuration system initialized successfully";
        public static final String SHUTTING_DOWN_CONFIG_SYSTEM = "Shutting down Nova configuration system";
        public static final String CONFIG_SYSTEM_SHUTDOWN = "Nova configuration system shut down successfully";
        public static final String SAVING_ALL_CONFIGS = "Saving all module configurations";
        public static final String RELOADING_ALL_CONFIGS = "Reloading all configurations";
        public static final String RESETTING_MODULE_CONFIG = "Resetting configuration for module: {}";
        public static final String RESETTING_ALL_CONFIGS = "Resetting all configurations to defaults";
        
        private Logging() {
            // Private constructor to prevent instantiation
        }
    }
    
    private ConfigurationConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}