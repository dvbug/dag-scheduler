package com.dvbug.dag;

/**
 * {@link Dag<>}图状态
 */
public enum DagState {
    /**
     * 已经创建
     */
    CREATED,
    /**
     * 正在初始化内部节点和边
     */
    INITIALIZING,
    /**
     * 调度前准备
     */
    PREPARED,
    /**
     * 调度中
     */
    SCHEDULING,
    /**
     * 调度完毕
     */
    COMPLETED
}
