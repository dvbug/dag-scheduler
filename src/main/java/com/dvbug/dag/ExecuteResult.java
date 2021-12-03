package com.dvbug.dag;

import lombok.Getter;
import lombok.ToString;

/**
 * DAG节点执行结果
 * @param <R> 结果类型
 */
@Getter
@ToString
public class ExecuteResult<R> {
    private final R result;
    private final DagNodeInfo info;
    private final Throwable throwable;

    public ExecuteResult(DagNodeInfo info, R result) {
        this.info = info;
        if(result instanceof Throwable) {
            this.result = null;
            this.throwable = (Throwable) result;
        } else {
            this.result = result;
            throwable = null;
        }
    }

    public ExecuteResult(DagNodeInfo info, Throwable throwable) {
        this.result = null;
        this.info = info;
        this.throwable = throwable;
    }

    public boolean isSucceed() {
        return null == throwable;
    }
}
