package com.dvbug.dag;

/**
 * DAG图节点状态通知
 */
public interface DagNodeStateChanged {
    void onNodeStateChanged(DagNodeState oldState, DagNodeState newState, DagNodeInfo node);
}
