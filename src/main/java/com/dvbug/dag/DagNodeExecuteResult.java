package com.dvbug.dag;

import lombok.Getter;
import lombok.ToString;

/**
 * DAG节点执行结果
 *
 * @param <R> 结果类型
 */
@Getter
@ToString
final class DagNodeExecuteResult<R> {
    private final R result;
    private final DagNodeInfo info;
    private final TraceInfo trace;
    private final Throwable throwable;

    public DagNodeExecuteResult(DagNodeInfo info, TraceInfo trace, R result) {
        this.info = info;
        this.trace = trace;
        if (result instanceof Throwable) {
            this.result = null;
            this.throwable = (Throwable) result;
        } else {
            this.result = result;
            throwable = null;
        }
    }

    public DagNodeExecuteResult(DagNodeInfo info, TraceInfo trace, Throwable throwable) {
        this.result = null;
        this.info = info;
        this.trace = trace;
        this.throwable = throwable;
    }

    public boolean isSucceed() {
        return null == throwable;
    }
}
