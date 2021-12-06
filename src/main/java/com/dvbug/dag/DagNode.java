package com.dvbug.dag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.dvbug.dag.DagNodeStateTransition.*;

/**
 * DAG 图节点
 *
 * @param <T> {@link NodeBean}子类,节点内连业务逻辑
 */
@Slf4j
public class DagNode<T extends NodeBean<?>> implements Executable, RuntimeInitializable {
    @Getter
    private final DagNodeInfo info;
    @Getter
    private final T bean;
    @Getter
    @Setter(AccessLevel.MODULE)
    private int expectDependCount;
    @Getter
    @Setter(AccessLevel.MODULE)
    private DagNodeStateChanged stateChangedHandler;

    private final ThreadableField<TraceInfo> trace = new ThreadableField<>();
    private final ThreadableField<DagNodeState> state = new ThreadableField<>();
    private final ThreadableField<Throwable> nodeThrowable = new ThreadableField<>();

    public DagNode(T bean) {
        this(bean, Integer.MIN_VALUE);
    }

    public DagNode(T bean, long timeout) {
        this.bean = bean;
        this.info = new DagNodeInfo(String.format("node-%s", bean.getName()), timeout, bean.isRoot(), bean.isFinal());
        this.trace.set(new TraceInfo(this.info)); //todo bug TraceInfo属于动态信息,不应该在这里设置
    }

    // 由 DAG调用
    void init(Dag<?> graph) {
        this.info.setGraphId(graph.getGraphId());
        this.info.setMode(graph.getMode());
        if (this.info.getTimeout() == Integer.MIN_VALUE) {
            this.info.setTimeout(graph.getTimeout());
        }
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

    TraceInfo getTrace() {
        return trace.get();
    }

    Throwable getNodeThrowable() {
        return nodeThrowable.get();
    }

    public DagNodeState getState() {
        return state.get();
    }


    @Override
    public void beforeRuntime() {
        this.trace.beforeRuntime();
        this.state.beforeRuntime();
        this.nodeThrowable.beforeRuntime();
        this.getBean().beforeRuntime();
    }

    @Override
    public void afterRuntime() {
        this.trace.afterRuntime();
        this.state.afterRuntime();
        this.nodeThrowable.afterRuntime();
        this.getBean().afterRuntime();
    }

    @Override
    public void reset() {
        this.trace.reset();
        this.state.reset();
        this.nodeThrowable.reset();
        this.getBean().reset();

        this.trace.set(new TraceInfo(this.info));
        this.state.set(DagNodeState.CREATED);
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
                    callback.onCompleted(new DagNodeExecuteResult<>(info, getTrace(), bean.getResult()));
                } else {
                    setState(DagNodeState.FAILED);
                    callback.onCompleted(new DagNodeExecuteResult<>(info, getTrace(), bean.getThrowable()));
                }
                nodeExecuteOk = true;
                break;
            } else {
                if (canIneffectiveInMode()) { // 模式判断是否可以INEFFECTIVE
                    setState(DagNodeState.INEFFECTIVE);
                    callback.onCompleted(new DagNodeExecuteResult<>(info, getTrace(), new IllegalStateException(String.format("%s node ineffective", info.getName()))));
                    nodeExecuteOk = true;
                    break;
                }

                //不可以RUNNING 则进行等待
                setState(DagNodeState.WAITING);
                // 不进行线程sleep 避免线程切换开销
//                try {
//                    setState(DagNodeState.WAITING);
//                    sleep(1);
//                } catch (InterruptedException e) {
//                    setState(DagNodeState.FAILED);
//                    e.printStackTrace();
//                    nodeThrowable.set(e);
//                    callback.onCompleted(new DagNodeExecuteResult<>(info, getTrace(), e));
//                    nodeExecuteOk = false;
//                    break;
//                }
            }
            expired = System.currentTimeMillis() - getTrace().getStateTime(DagNodeState.START);
        }

        //在非异常情况下引发节点超时
        if (expired > info.getTimeout() && !nodeExecuteOk) {
            setState(DagNodeState.TIMEOUT);
            callback.onCompleted(new DagNodeExecuteResult<>(info, getTrace(), new IllegalStateException(String.format("%s node timeout", info.getName()))));
        }

        onCompletedExecute();

        //节点是否正常调度运行(与业务无关)
        return nodeExecuteOk;
    }

    public boolean isRunning() {
        DagNodeState state = getState();
        return state == DagNodeState.RUNNING;
    }

    public boolean isScheduled() {
        DagNodeState state = getState();
        return null != state && state != DagNodeState.CREATED;
    }

    public boolean isFinished() {
        DagNodeState state = getState();
        return null == state || isFinalState(state);
    }

    @Override
    public String toString() {
        return String.format("%s[%s, %s]", getClass().getSimpleName(), info.getName(), getState());
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
        return (-1 == info.getTimeout() || expired <= info.getTimeout()) && maybeTransAllow(getState(), DagNodeState.RUNNING);
    }

    private boolean canRunningInMode() {
        switch (info.getMode()) {
            case PARALLEL:
                return bean.getParamCount() >= expectDependCount && bean.executeEnable();
            case SWITCH:
                return bean.getParamCount() > 0 && bean.executeEnable();
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
        if (null != getState() && getState().equals(state)) {
            return;
        }
        DagNodeState oldState = getState();
        if (!transAllow(oldState, state)) {
            return;
        }
        this.state.set(state);
        getTrace().setFinalState(state);
        if (null != stateChangedHandler) {
            stateChangedHandler.onNodeStateChanged(oldState, state, info);
        }
    }

    private void printParamsCount() {
        log.debug("{}, param depend expect={}, actual={}", this, expectDependCount, bean.getParamCount());
    }
}

