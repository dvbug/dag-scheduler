package com.dvbug.dag;

/**
 * DAG节点执行完毕回调
 */
public interface ExecuteCallback {
    <R> void onCompleted(ExecuteResult<R> executeResult);
}
