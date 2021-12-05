package com.dvbug.dag;

/**
 * DAG 生命周期内事件监听器
 */
public interface DagEventHandler {
    /**
     * {@link Dag}图被创建
     *
     * @param graph {@link Dag}实例
     */
    void onCreated(Dag<?> graph);

    /**
     * {@link DagNode}被添加到{@link Dag}实例图中
     *
     * @param node  被添加的 {@link DagNode} 实例
     * @param graph {@link Dag}实例
     */
    void onNodeAdded(DagNode<? extends NodeBean<?>> node, Dag<?> graph);

    /**
     * 由两个{@link DagNode}表示的DAG边路径被添加到{@link Dag}实例图中
     *
     * @param from  DAG边路{@link DagNode}起始节点(被 {@param to} 依赖节点)
     * @param to    DAG边路{@link DagNode}结束节点(为 {@param from} 的下游节点)
     * @param graph {@link Dag}实例
     */
    void onEdgeAdded(DagNode<? extends NodeBean<?>> from, DagNode<? extends NodeBean<?>> to, Dag<?> graph);

    /**
     * {@link Dag}图被调度器{@link DagScheduler}进行通知,调度前准备
     *
     * @param graph {@link Dag}实例
     */
    void onPrepared(Dag<?> graph);

    /**
     * {@link Dag}图被调度器{@link DagScheduler}开始调度
     *
     * @param graph {@link Dag}实例
     */
    void onScheduling(Dag<?> graph);

    /**
     * {@link Dag}图被调度器{@link DagScheduler}调度结束
     *
     * @param graph {@link Dag}实例
     */
    void onCompleted(Dag<?> graph);
}
