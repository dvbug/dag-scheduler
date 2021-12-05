package com.dvbug.dag;

/**
 * DAG节点内连Bean对象
 */
public interface NodeBean<T> {

    boolean isRoot();

    boolean isFinal();

    String getName();

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
    boolean executeAble();


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
