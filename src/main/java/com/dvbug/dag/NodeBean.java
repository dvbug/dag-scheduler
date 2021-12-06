package com.dvbug.dag;

/**
 * DAG节点内连Bean对象
 */
public interface NodeBean<T> extends RuntimeInitializable {

    boolean isRoot();

    boolean isFinal();

    String getName();

    /**
     * 对象声明周期激活,此时对象运行时的线程已经确定
     * 可以在此进行一些线程相关的初始化工作
     * 在{@link NodeBean#execute()}和{@link NodeBean#executeEnable()}方法调用前被执行
     */
    void beforeRuntime();

    /**
     * 对象声明周期即将结束
     * 可以在此进行一些线程相关的资源释放工作
     * 方法被调用在{@link NodeBean#execute()}结束之后
     */
    void afterRuntime();

    /**
     * 业务方关注<br/>
     * 核心执行动作方法<br/>
     * <p>
     * 子类如果执行成功,需要调用 {@link NodeBean#setResult(Object)} 设置执行结果<br/>
     * 子类如果执行失败,需要调用 {@link NodeBean#setThrowable(Throwable)} 设置执行异常<br/>
     * 子类如果抛出异常,需要调用 {@link NodeBean#setThrowable(Throwable)} 设置执行异常,并返回 false<br/>
     *
     * @return 返回是否执行成功
     */
    boolean execute();

    /**
     * 业务方关注<br/>
     * 内连Bean对象是否可以执行{@link NodeBean#execute()}方法<br/>
     * 通常在本方法内部进行可执行的参数和条件是否满足<br/>
     */
    boolean executeEnable();


    /**
     * 业务方关注<br/>
     * 业务方必须在{@link NodeBean#execute()}方法返回true前调用
     *
     * @param result {@link T}的实例
     */
    void setResult(T result);

    /**
     * 业务方关注<br/>
     * 业务方必须在{@link NodeBean#execute()}方法返回false前调用
     *
     * @param throwable {@link Throwable}的实例
     */
    void setThrowable(Throwable throwable);

    T getResult();

    Throwable getThrowable();

    void setParam(Object param);

    int getParamCount();

    boolean equals(Object o);

    int hashCode();
}
