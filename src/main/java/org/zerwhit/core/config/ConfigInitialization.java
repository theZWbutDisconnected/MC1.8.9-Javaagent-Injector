package org.zerwhit.core.config;

import org.zerwhit.core.module.ModuleBase;
import org.zerwhit.core.util.SafeLogger;

public final class ConfigInitialization {
    
    private static final SafeLogger LOGGER = SafeLogger.getLogger(ConfigInitialization.class);
    private static volatile boolean initialized = false;
    
    private ConfigInitialization() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    public static void initialize() {
        if (isInitialized()) {
            LOGGER.debug("Configuration system already initialized, skipping initialization");
            return;
        }
        
        synchronized (ConfigInitialization.class) {
            if (isInitialized()) {
                return;
            }
            
            try {
                LOGGER.info("Starting Nova configuration system initialization");
                performInitialization();
                initialized = true;
                LOGGER.info("Nova configuration system initialized successfully");
            } catch (Exception e) {
                handleInitializationFailure(e);
            }
        }
    }
    
    private static void performInitialization() throws Exception {
        ConfigManager.getInstance().initialize();
        ModuleBase.reloadAllConfigurations();
    }
    
    private static void handleInitializationFailure(Exception e) {
        String errorMessage = String.format("Failed to initialize configuration system: %s", e.getMessage());
        LOGGER.error(errorMessage, e);
        initialized = false;
        throw new ConfigurationInitializationException(errorMessage, e);
    }
    
    public static void shutdown() {
        if (!isInitialized()) {
            LOGGER.debug("Configuration system not initialized, skipping shutdown");
            return;
        }
        
        synchronized (ConfigInitialization.class) {
            if (!isInitialized()) {
                return;
            }
            
            try {
                LOGGER.info("Starting Nova configuration system shutdown");
                performShutdown();
                initialized = false;
                LOGGER.info("Nova configuration system shut down successfully");
            } catch (Exception e) {
                handleShutdownFailure(e);
            }
        }
    }
    
    private static void performShutdown() throws Exception {
        ConfigManager.getInstance().saveAllConfigurations();
    }
    
    private static void handleShutdownFailure(Exception e) {
        String errorMessage = String.format("Error during configuration system shutdown: %s", e.getMessage());
        LOGGER.error(errorMessage, e);
        throw new ConfigurationShutdownException(errorMessage, e);
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static class ConfigurationInitializationException extends RuntimeException {

        public ConfigurationInitializationException(String message) {
            super(message);
        }
        
        public ConfigurationInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class ConfigurationShutdownException extends RuntimeException {
        
        public ConfigurationShutdownException(String message) {
            super(message);
        }
        
        public ConfigurationShutdownException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}