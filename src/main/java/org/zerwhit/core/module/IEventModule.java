package org.zerwhit.core.module;

/**
 * 事件模块接口
 * 用于处理特定事件的模块
 */
public interface IEventModule {
    /**
     * 处理事件
     * @param eventType 事件类型
     * @param args 事件参数
     */
    void onEvent(String eventType, Object... args);
}