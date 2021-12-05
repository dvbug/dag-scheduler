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
 * @param <T> {@link NodeBean}子类,节点内连业务逻辑
 */
@Getter
@Slf4j
public class DagNode<T extends NodeBean<?>> implements ExecuteAble {
    private Dag<?> graph;
    private final DagNodeInfo info;
    private final TraceInfo trace;
    private final T bean;
    private DagNodeState state;
    @Setter(AccessLevel.MODULE)
    private int expectDependCount;
    @Setter(AccessLevel.MODULE)
    private DagNodeStateChanged stateChanged;
    @Getter(AccessLevel.MODULE)
    private Throwable nodeThrowable;

    public DagNode(T bean) {
        this(bean, -1);
    }

    public DagNode(T bean, long timeout) {
        this.bean = bean;
        this.info = new DagNodeInfo(String.format("node-%s", bean.getName()), timeout);
        this.trace = new TraceInfo();
    }

    // 由 DAG调用
    void init(String graphId, DagMode mode, Dag<?> graph) {
        this.info.setGraphId(graphId);
        this.info.setMode(mode);
        this.graph = graph;
        setState(DagNodeState.CREATED);
        onAfterInit();
    }

    // 由 DAG调度器调用
    void putParam(Object param) {
        bean.setParam(param);
        printParamsCount();
    }

    // 由 DAG调度器调用
    void setPrepared() {
        setState(DagNodeState.PREPARED);
    }

    // 由 DAG调度器调用
    void notifyDependFail(DagNode<? extends NodeBean<?>> depend) {
        getTrace().getFailedDepends().add(depend);
    }

    @Override
    public final boolean execute(DagNodeExecutionCallback callback) {
        onBeforeExecute();
        setState(DagNodeState.START);

        boolean nodeExecuteOk = false;
        long expired = 0;
        while (maybeCanRunning(expired)) { //是否有必要继续循环等待进入RUNNING
            printParamsCount();
            if (canRunningInMode()) { // 模式判断是否可以RUNNING
                setState(DagNodeState.RUNNING);
                if (bean.execute()) {
                    setState(DagNodeState.SUCCESS);
                    callback.onCompleted(new DagNodeExecuteResult<>(info, trace, bean.getResult()));
                } else {
                    setState(DagNodeState.FAILED);
                    callback.onCompleted(new DagNodeExecuteResult<>(info, trace, bean.getThrowable()));
                }
                nodeExecuteOk = true;
                break;
            } else {
                if (canIneffectiveInMode()) { // 模式判断是否可以INEFFECTIVE
                    setState(DagNodeState.INEFFECTIVE);
                    callback.onCompleted(new DagNodeExecuteResult<>(info, trace, new IllegalStateException(String.format("%s node ineffective", info.getName()))));
                    nodeExecuteOk = true;
                    break;
                }

                //不可以RUNNING 则进行等待
                try {
                    setState(DagNodeState.WAITING);
                    sleep(1);
                } catch (InterruptedException e) {
                    setState(DagNodeState.FAILED);
                    e.printStackTrace();
                    nodeThrowable = e;
                    callback.onCompleted(new DagNodeExecuteResult<>(info, trace, e));
                    nodeExecuteOk = false;
                    break;
                }
            }
            expired = System.currentTimeMillis() - getTrace().getStateTime(DagNodeState.START);
        }

        //在非异常情况下引发节点超时
        if (expired > info.getTimeout() && !nodeExecuteOk) {
            setState(DagNodeState.TIMEOUT);
            callback.onCompleted(new DagNodeExecuteResult<>(info, trace, new IllegalStateException(String.format("%s node timeout", info.getName()))));
        }

        onCompletedExecute();

        //节点是否正常调度运行(与业务无关)
        return nodeExecuteOk;
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

    protected void onAfterInit() {
    }

    protected void onBeforeExecute() {
    }

    protected void onCompletedExecute() {
    }

    private boolean maybeCanRunning(long expired) {
        return (-1 == info.getTimeout() || expired <= info.getTimeout()) && maybeTransAllow(state, DagNodeState.RUNNING);
    }

    private boolean canRunningInMode() {
        switch (info.getMode()) {
            case PARALLEL:
                return bean.getParamCount() >= expectDependCount && bean.executeAble();
            case SWITCH:
                return bean.getParamCount() > 0 && bean.executeAble();
            default:
                return false;
        }
    }

    private boolean canIneffectiveInMode() {
        switch (info.getMode()) {
            case PARALLEL:
                return getTrace().getFailedDepends().size() > 0;
            case SWITCH:
                return getTrace().getFailedDepends().size() >= expectDependCount;
            default:
                return false;
        }
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
        if (null != stateChanged) {
            stateChanged.onNodeStateChanged(oldState, state, info);
        }
    }

    private void printParamsCount() {
        log.debug("{}, param depend expect={}, actual={}", this, expectDependCount, bean.getParamCount());
    }
}

