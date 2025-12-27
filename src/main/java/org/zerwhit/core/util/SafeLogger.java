package org.zerwhit.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Safe logger that handles potential failures in log4j2 initialization
 * and provides fallback logging to System.out/err
 */
public class SafeLogger {
    private final Logger logger;
    private final String name;
    private static boolean log4jAvailable = true;
    
    static {
        try {
            Logger testLogger = LogManager.getLogger(SafeLogger.class);
        } catch (Exception e) {
            System.err.println("Log4j2 initialization failed, falling back to System.out/err: " + e.getMessage());
            log4jAvailable = false;
        }
    }
    
    private SafeLogger(Class<?> clazz) {
        this.name = clazz.getSimpleName();
        Logger tempLogger = null;
        try {
            if (log4jAvailable) {
                tempLogger = LogManager.getLogger(clazz);
            }
        } catch (Exception e) {
            System.err.println("Failed to create logger for " + name + ": " + e.getMessage());
        }
        this.logger = tempLogger;
    }
    
    private SafeLogger(String name) {
        this.name = name;
        Logger tempLogger = null;
        try {
            if (log4jAvailable) {
                tempLogger = LogManager.getLogger(name);
            }
        } catch (Exception e) {
            System.err.println("Failed to create logger for " + name + ": " + e.getMessage());
        }
        this.logger = tempLogger;
    }
    
    public static SafeLogger getLogger(Class<?> clazz) {
        return new SafeLogger(clazz);
    }
    
    public static SafeLogger getLogger(String name) {
        return new SafeLogger(name);
    }
    
    public void info(String message) {
        try {
            if (logger != null && log4jAvailable) {
                logger.info(message);
            } else {
                System.out.println("[INFO] [" + name + "] " + message);
            }
        } catch (Exception e) {
            System.out.println("[INFO] [" + name + "] " + message + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void info(String message, Object... params) {
        try {
            if (logger != null && log4jAvailable) {
                logger.info(message, params);
            } else {
                System.out.println("[INFO] [" + name + "] " + String.format(message.replace("{}", "%s"), params));
            }
        } catch (Exception e) {
            System.out.println("[INFO] [" + name + "] " + String.format(message.replace("{}", "%s"), params) + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void debug(String message) {
        try {
            if (logger != null && log4jAvailable) {
                logger.debug(message);
            } else {
                System.out.println("[DEBUG] [" + name + "] " + message);
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] [" + name + "] " + message + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void debug(String message, Object... params) {
        try {
            if (logger != null && log4jAvailable) {
                logger.debug(message, params);
            } else {
                System.out.println("[DEBUG] [" + name + "] " + String.format(message.replace("{}", "%s"), params));
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] [" + name + "] " + String.format(message.replace("{}", "%s"), params) + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void warn(String message) {
        try {
            if (logger != null && log4jAvailable) {
                logger.warn(message);
            } else {
                System.out.println("[WARN] [" + name + "] " + message);
            }
        } catch (Exception e) {
            System.out.println("[WARN] [" + name + "] " + message + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void warn(String message, Object... params) {
        try {
            if (logger != null && log4jAvailable) {
                logger.warn(message, params);
            } else {
                System.out.println("[WARN] [" + name + "] " + String.format(message.replace("{}", "%s"), params));
            }
        } catch (Exception e) {
            System.out.println("[WARN] [" + name + "] " + String.format(message.replace("{}", "%s"), params) + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void error(String message) {
        try {
            if (logger != null && log4jAvailable) {
                logger.error(message);
            } else {
                System.err.println("[ERROR] [" + name + "] " + message);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] [" + name + "] " + message + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void error(String message, Object... params) {
        try {
            if (logger != null && log4jAvailable) {
                logger.error(message, params);
            } else {
                System.err.println("[ERROR] [" + name + "] " + String.format(message.replace("{}", "%s"), params));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] [" + name + "] " + String.format(message.replace("{}", "%s"), params) + " (log4j2 error: " + e.getMessage() + ")");
        }
    }
    
    public void error(String message, Throwable throwable) {
        try {
            if (logger != null && log4jAvailable) {
                logger.error(message, throwable);
            } else {
                System.err.println("[ERROR] [" + name + "] " + message);
                if (throwable != null) {
                    System.err.print("[ERROR] [" + name + "] Exception: ");
                    throwable.printStackTrace(System.err);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] [" + name + "] " + message + " (log4j2 error: " + e.getMessage() + ")");
            if (throwable != null) {
                System.err.print("[ERROR] [" + name + "] Original exception: ");
                throwable.printStackTrace(System.err);
            }
        }
    }
}