package com.dvbug.dag;

/**
 * DAG图节点状态通知
 */
public interface DagNodeStateChange {
    void changed(DagNodeState oldState, DagNodeState newState, DagNodeInfo node);
}
