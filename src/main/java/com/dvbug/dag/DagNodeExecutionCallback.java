package com.dvbug.dag;

/**
 * DAG节点执行完毕回调
 */
interface DagNodeExecutionCallback {
    <R> void onCompleted(DagNodeExecuteResult<R> executeResult);
}
