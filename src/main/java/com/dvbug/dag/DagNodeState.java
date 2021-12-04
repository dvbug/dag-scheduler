package com.dvbug.dag;

/**
 * DAG图节点状态
 */
public enum DagNodeState {
    /**
     * 节点已经创建
     */
    CREATED,
    /**
     * 准备开始调度
     */
    PREPARED,
    /**
     * 开始调度
     */
    START,
    /**
     * 等待执行
     */
    WAITING,
    /**
     * 正在执行
     */
    RUNNING,
    /**
     * 执行成功
     */
    SUCCESS,
    /**
     * 执行失败
     */
    FAILED,
    /**
     * 无效节点(父节点失败后下游单依赖子节点全部无效)
     */
    INEFFECTIVE
}
