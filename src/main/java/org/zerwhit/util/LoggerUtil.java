package org.zerwhit.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 日志工具类，用于统一管理日志记录
 */
public class LoggerUtil {
    
    /**
     * 获取指定类的Logger实例
     * @param clazz 类
     * @return Logger实例
     */
    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }
    
    /**
     * 获取指定名称的Logger实例
     * @param name 日志器名称
     * @return Logger实例
     */
    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }
    
    /**
     * 记录错误信息并打印异常堆栈
     * @param logger Logger实例
     * @param message 错误信息
     * @param throwable 异常对象
     */
    public static void errorWithStackTrace(Logger logger, String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    /**
     * 记录警告信息并打印异常堆栈
     * @param logger Logger实例
     * @param message 警告信息
     * @param throwable 异常对象
     */
    public static void warnWithStackTrace(Logger logger, String message, Throwable throwable) {
        logger.warn(message, throwable);
    }
}