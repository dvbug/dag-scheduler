package com.dvbug.dag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.dvbug.dag.DagNodeStateTransition.*;
import static java.lang.Thread.sleep;

/**
 * DAG 图节点
 *
 * @param <T> {@link NodeBean}子类
 */
@Getter
@Slf4j
public class DagNode<T extends NodeBean> implements Executor {

    private final DagNodeInfo info;
    private final TraceInfo trace;
    private final T bean;
    @Setter(AccessLevel.MODULE)
    private DagMode mode;
    private DagNodeState state;
    @Setter(AccessLevel.MODULE)
    private int expectDependCount;
    @Setter
    private DagNodeStateChange stateChange;
    @Getter(AccessLevel.MODULE)
    private Throwable nodeThrowable;

    public DagNode(T bean) {
        this(bean, -1);
    }

    public DagNode(T bean, long timeout) {
        this.bean = bean;

        this.info = new DagNodeInfo(
                String.format("%s-%s", System.currentTimeMillis(), bean.hashCode()),
                String.format("node-%s", bean.getName()),
                timeout);
        this.trace = new TraceInfo();
        setState(DagNodeState.CREATED);
        onAfterInit();
    }

    public void putParam(Object param) {
        bean.setParam(param);
        printParamsCount();
    }

    private void printParamsCount() {
        log.debug("{}, param depend expect={}, actual={}", this, expectDependCount, bean.getParamCount());
    }

    private void setState(DagNodeState state) {
        if (null != this.state && this.state.equals(state)) {
            return;
        }
        DagNodeState oldState = this.state;
        if (!transAllow(oldState, state)) {
            return;
        }
        this.state = state;
        getTrace().setFinalState(state);
        if (null != stateChange) {
            stateChange.changed(oldState, state, info);
        }
    }

    @Override
    public final boolean execute(ExecuteCallback callback) {
        log.debug("{} is start schedule", this);

        onBeforeExecute();
        setState(DagNodeState.START);

        boolean nodeExecuteOk = true;
        long expired = 0;
        while (maybeCanRunning(expired)) {
            printParamsCount();
            if (canRunningInMode()) {
                setState(DagNodeState.RUNNING);
                if (bean.execute()) {
                    setState(DagNodeState.SUCCESS);
                    callback.onCompleted(new ExecuteResult<>(info, trace, bean.getResult()));
                } else {
                    setState(DagNodeState.FAILED);
                    callback.onCompleted(new ExecuteResult<>(info, trace, bean.getThrowable()));
                }
                break;
            } else {
                if (canIneffectiveInMode()) {
                    setState(DagNodeState.INEFFECTIVE);
                    callback.onCompleted(new ExecuteResult<>(info, trace, new IllegalStateException(String.format("%s node ineffective", info.getName()))));
                    break;
                }

                try {
                    setState(DagNodeState.WAITING);
                    sleep(1);
                } catch (InterruptedException e) {
                    setState(DagNodeState.FAILED);
                    e.printStackTrace();
                    nodeThrowable = e;
                    callback.onCompleted(new ExecuteResult<>(info, trace, e));
                    onCompleteExecute();
                    nodeExecuteOk = false;
                    break;
                }
            }
            expired = System.currentTimeMillis() - getTrace().getStateTime(DagNodeState.START);
        }

        return nodeExecuteOk;
    }

    private boolean maybeCanRunning(long expired) {
        return (-1 == info.getTimeout() || expired <= info.getTimeout()) && maybeTransAllow(state, DagNodeState.RUNNING);
    }

    private boolean canRunningInMode() {
        switch (mode) {
            case PARALLEL:
                return bean.getParamCount() >= expectDependCount && bean.executeAble();
            case SWITCH:
                return bean.getParamCount() > 0 && bean.executeAble();
            default:
                return false;
        }
    }

    private boolean canIneffectiveInMode() {
        switch (mode) {
            case PARALLEL:
                return getTrace().getFailedDepends().size() > 0;
            case SWITCH:
                return getTrace().getFailedDepends().size() >= expectDependCount;
            default:
                return false;
        }
    }

    public boolean isRunning() {
        return state == DagNodeState.RUNNING;
    }

    public boolean isScheduled() {
        return state != DagNodeState.CREATED;
    }

    public boolean isFinished() {
        return isFinalState(state);
    }

    void setPrepared() {
        setState(DagNodeState.PREPARED);
    }

    void notifyDependFail(DagNode<? extends NodeBean> depend) {
        getTrace().getFailedDepends().add(depend);
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s]", getClass().getSimpleName(), info.getName(), state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DagNode<?> dagNode = (DagNode<?>) o;
        return info.equals(dagNode.info) && bean.equals(dagNode.bean);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info, bean);
    }

    public void onAfterInit() {
    }

    public void onBeforeExecute() {
    }

    public void onCompleteExecute() {
    }
}

