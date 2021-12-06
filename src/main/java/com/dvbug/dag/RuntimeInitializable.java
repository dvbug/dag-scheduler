package com.dvbug.dag;

public interface RuntimeInitializable {
    /**
     * 对象声明周期激活,此时对象运行时的线程已经确定
     * 可以在此进行一些线程相关的初始化工作
     */
    void beforeRuntime();

    /**
     * 对象声明周期即将结束
     * 可以在此进行一些线程相关的资源释放工作
     */
    void afterRuntime();

    void reset();
}
