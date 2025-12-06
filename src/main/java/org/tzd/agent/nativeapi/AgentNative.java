package org.tzd.agent.nativeapi;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.UnmodifiableClassException;

/**
 * Agent 本地接口类 (JNI Bridge)。
 * <p>
 * 该类作为 Java 这里的 Agent 与底层 C/C++ 实现的桥梁，
 * 定义了 Agent 的上下文句柄结构以及核心功能的本地方法声明。
 * </p>
 *
 * @author tzd
 */
public class AgentNative {

    /**
     * Agent 句柄 (Handle)。
     * <p>
     * 用于在 Java 和 Native 层之间传递 Agent 的状态、JVM 信息以及当前绑定的转换器。
     * 该对象通常由 Native 层构建并传递给 Java 层。
     * </p>
     */
    public static class AgentHandle {
        /**
         * Native 层对应的指针地址或句柄 ID。
         */
        private long handle;

        /**
         * 目标进程 ID (PID)。
         */
        private long pid;

        /**
         * JVM 实现名称 (e.g., HotSpot)。
         */
        private String jvm;

        /**
         * 进程名称。
         */
        private String processName;

        /**
         * JVM 版本信息。
         */
        private String jvmVersion;

        /**
         * 标记 Agent 是否正在启用/激活中。
         */
        public boolean isEnabling;

        /**
         * 当前封装的类文件转换器 (ClassFileTransformer)。
         */
        private ClassFileTransformerEncapsulation transformer;

        @Override
        public String toString() {
            return "AgentHandle{" +
                    "handle=" + handle +
                    ", pid=" + pid +
                    ", jvm='" + jvm + '\'' +
                    ", processName='" + processName + '\'' +
                    ", jvmVersion='" + jvmVersion + '\'' +
                    ", isEnabling=" + isEnabling +
                    ", transformer=" + (transformer != null ? transformer.getClass().getSimpleName() : "null") +
                    '}';
        }
    }

    /**
     * ClassFileTransformer 的封装类。
     * <p>
     * 这里的封装不仅包含标准的 {@link ClassFileTransformer}，还包含了转换器的名称
     * 以及是否支持重转换 (Retransform) 的元数据。
     * </p>
     */
    public static class ClassFileTransformerEncapsulation {
        /**
         * 转换器的名称，默认为 "[SecretName]"。
         * 用于在日志或调试中标识转换器。
         */
        private String name = "[SecretName]";

        /**
         * 实际执行字节码转换逻辑的接口实例。
         */
        private ClassFileTransformer transformer = null;

        /**
         * 标记是否允许重转换 (Retransform)。
         * 如果为 true，则允许在类加载后通过 retransformClasses 再次触发转换。
         */
        private boolean canRetransform = false;

        /**
         * 全参构造函数。
         *
         * @param name           转换器名称
         * @param transformer    转换器实例
         * @param canRetransform 是否支持重转换
         */
        public ClassFileTransformerEncapsulation(String name, ClassFileTransformer transformer, boolean canRetransform) {
            this.name = name;
            this.transformer = transformer;
            this.canRetransform = canRetransform;
        }

        /**
         * 构造函数 (使用默认名称)。
         *
         * @param transformer    转换器实例
         * @param canRetransform 是否支持重转换
         */
        public ClassFileTransformerEncapsulation(ClassFileTransformer transformer, boolean canRetransform) {
            this.transformer = transformer;
            this.canRetransform = canRetransform;
        }

        /**
         * 构造函数 (使用默认名称，且默认不支持重转换)。
         *
         * @param transformer 转换器实例
         */
        public ClassFileTransformerEncapsulation(ClassFileTransformer transformer) {
            this.transformer = transformer;
        }
    }

    /**
     * 初始化 Agent 并注入到目标进程。
     * <p>
     * 这是一个 Native 方法，通常会在底层调用 JVMTI 的 Attach API。
     * </p>
     *
     * @param pid           需要注入的目标进程 PID (Process ID)。
     * @param jarPath       注入的 Agent Jar 包的绝对路径。
     * @param agentLocation Agent 入口类的全限定名（例如：org.tzd.agent.Main）。
     * @param args          传递给 Agent 的参数数组。
     * @return 如果启动成功返回 null 或空字符串，否则返回具体的错误信息。
     */
    public static native String agent_init(long pid, String jarPath, String agentLocation, String[] args);

    /**
     * 重定义类 (Redefine Classes)。
     * <p>
     * 允许使用提供的字节码重新定义现有的类。此操作通常用于热修复 (HotSwap)，
     * 但不会触发类的初始化器。
     * </p>
     *
     * @param handle           Agent 句柄，包含上下文信息。
     * @param classDefinitions 包含类对象和新字节码定义的变长参数。
     * @throws ClassNotFoundException     如果找不到指定的类。
     * @throws UnmodifiableClassException 如果尝试修改一个不可修改的类。
     */
    public static native void redefineClasses(AgentHandle handle, ClassDefinition... classDefinitions)
            throws ClassNotFoundException, UnmodifiableClassException;

    /**
     * 重新转换类 (Retransform Classes)。
     * <p>
     * 触发 JVM 重新读取类文件，并调用所有注册了 canRetransform=true 的 Transformer。
     * 这允许在不提供新字节码的情况下，让 Transformer 重新处理已加载的类。
     * </p>
     *
     * @param handle  Agent 句柄，包含上下文信息。
     * @param classes 需要重新转换的类列表。
     * @throws ClassNotFoundException     如果找不到指定的类。
     * @throws UnmodifiableClassException 如果尝试转换一个不可修改的类。
     */
    public static native void retransformClasses(AgentHandle handle, Class<?>... classes)
            throws ClassNotFoundException, UnmodifiableClassException;

    /**
     * 添加 ClassFileTransformer。
     * <p>
     * 将 Java 定义的 Transformer 注册到 Native 层的 Agent 中，以便在类加载或重转换时被调用。
     * </p>
     *
     * @param handle      Agent 句柄。
     * @param transformer 封装后的 Transformer 对象。
     * @throws Exception 如果注册过程中发生错误。
     */
    public static native void addTransformer(AgentHandle handle, ClassFileTransformerEncapsulation transformer) throws Exception;

    /**
     * 获取指定类的所有实例对象
     * <p>
     * 警告：此操作会遍历堆内存，在对象非常多的大堆上可能会有性能损耗。
     * </p>
     *
     * @param clazz 目标类
     * @return 该类的所有实例数组
     */
    public static native Object[] getInstances(Class<?> clazz);
}