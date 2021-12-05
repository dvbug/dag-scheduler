package com.dvbug.dag;

import com.dvbug.strategy.FinalStrategy;
import com.dvbug.strategy.RootStrategy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static com.dvbug.dag.DagStateTransition.transAllow;

@Slf4j
@Getter
public class Dag<R> implements DagEventHandler, DagNodeStateChanged {
    private final DagMode mode;
    private final String graphId;
    @Getter(AccessLevel.NONE)
    private DagState state;
    private int edgeCount = 0;
    private DagNode<? extends NodeBean<?>> rootDagNode;
    private DagNode<? extends NodeBean<?>> finalDagNode;
    private final Set<DagNode<? extends NodeBean<?>>> dagNodes;
    private final Map<DagNode<? extends NodeBean<?>>, Set<DagNode<? extends NodeBean<?>>>> depends;
    private final Map<DagNode<? extends NodeBean<?>>, Set<DagNode<? extends NodeBean<?>>>> children;
    private final DagEventHandler eventHandler;

    /**
     * 创建指定模式的DAG图
     *
     * @param mode DAG图模式
     */
    public Dag(DagMode mode) {
        this(mode, null);
    }

    /**
     * 创建指定模式的DAG图并指定事件监听器
     *
     * @param mode         DAG图模式
     * @param eventHandler DAG图生命周期内事件监听器
     */
    public Dag(DagMode mode, DagEventHandler eventHandler) {
        this(mode, null, null, eventHandler);
    }

    /**
     * 创建指定模式的DAG图并指定起始输入参数类型和最终输出参数类型
     *
     * @param mode       DAG图模式
     * @param inputType  起始输入参数类型
     * @param resultType 最终输出参数类型
     */
    public Dag(DagMode mode, Class<?> inputType, Class<R> resultType) {
        this(mode, inputType, resultType, null);
    }

    /**
     * 创建指定模式的DAG图并指定起始输入参数类型、最终输出参数类型和事件监听器
     *
     * @param mode         DAG图模式
     * @param inputType    起始输入参数类型
     * @param resultType   最终输出参数类型
     * @param eventHandler DAG图生命周期内事件监听器
     */
    public Dag(DagMode mode, Class<?> inputType, Class<R> resultType, DagEventHandler eventHandler) {
        this(UUID.randomUUID().toString().replaceAll("-", ""), mode, inputType, resultType, eventHandler);
    }

    private Dag(String graphId, DagMode mode, Class<?> inputType, Class<R> resultType, DagEventHandler eventHandler) {
        this.mode = mode;
        this.graphId = graphId;
        this.dagNodes = new HashSet<>();
        this.depends = new HashMap<>();
        this.children = new HashMap<>();
        if (null != eventHandler) {
            this.eventHandler = eventHandler;
        } else {
            this.eventHandler = this;
        }

        setState(DagState.CREATED);

        if (null != inputType) {
            this.rootDagNode = new DagNode<>(new RootStrategy(inputType));
            addNode(this.rootDagNode);
        }
        if (null != resultType) {
            this.finalDagNode = new DagNode<>(new FinalStrategy(resultType));
            addNode(this.finalDagNode);
        }
    }

    public void addNode(NodeBean<?> nodeBean) {
        if (dagNodes.stream().anyMatch(n -> n.getBean().getName().equals(nodeBean.getName()))) {
            throw new IllegalArgumentException(String.format("%s is existed in graph[%s]", nodeBean, graphId));
        }
        addNode(new DagNode<>(nodeBean));
    }

    public void addNode(DagNode<? extends NodeBean<?>> dagNode) {
        if (dagNodes.contains(dagNode)) {
            throw new IllegalArgumentException(String.format("%s is existed in graph[%s]", dagNode, graphId));
        }
        if (dagNode.getBean().isRoot() && dagNodes.stream().anyMatch(t -> t.getBean().isRoot())) {
            throw new IllegalArgumentException(String.format("Just set root once in graph[%s]", graphId));
        }

        if (dagNode.getBean().isFinal() && dagNodes.stream().anyMatch(t -> t.getBean().isFinal())) {
            throw new IllegalArgumentException(String.format("Just set final once in graph[%s]", graphId));
        }

        if (dagNode.getBean().isRoot()) {
            rootDagNode = dagNode;
        }
        if (dagNode.getBean().isFinal()) {
            finalDagNode = dagNode;
        }

        dagNode.init(graphId, mode, this);
        dagNodes.add(dagNode);

        setState(DagState.INITIALIZING);
        raiseEventOnNodeAdded(dagNode);
    }

    public void addEdge(String beanName, String beanNameDependOn) {
        Optional<DagNode<? extends NodeBean<?>>> beanNode = dagNodes.stream().filter(n -> n.getBean().getName().equals(beanName)).findFirst();
        Optional<DagNode<? extends NodeBean<?>>> dependNode = dagNodes.stream().filter(n -> n.getBean().getName().equals(beanNameDependOn)).findFirst();

        if (!beanNode.isPresent()) {
            throw new IllegalArgumentException(String.format("No %s bean named [%s] in graph[%s]", DagNode.class.getSimpleName(), beanName, graphId));
        }
        if (!dependNode.isPresent()) {
            throw new IllegalArgumentException(String.format("No %s bean named [%s] in graph[%s]", DagNode.class.getSimpleName(), beanNameDependOn, graphId));
        }

        addEdge(beanNode.get(), dependNode.get());
    }

    public void addEdge(DagNode<? extends NodeBean<?>> dagNode, DagNode<? extends NodeBean<?>> dependOn) {
        String edgeName = edgeName(dependOn, dagNode);

        if (!dagNodes.contains(dagNode) || !dagNodes.contains(dependOn)) {
            throw new IllegalStateException(String.format("Dag graph edge[%s] can not independent in graph[%s]", edgeName, graphId));
        }

        Set<DagNode<? extends NodeBean<?>>> depends = this.depends.computeIfAbsent(dagNode, k -> new HashSet<>());
        Set<DagNode<? extends NodeBean<?>>> children = this.children.computeIfAbsent(dependOn, k -> new HashSet<>());

        if (depends.contains(dependOn)) {
            throw new IllegalStateException(String.format("Dag graph edge[%s] is existed in graph[%s]", edgeName, graphId));
        }

        depends.add(dependOn);
        dagNode.setExpectDependCount(depends.size());
        children.add(dagNode);
        edgeCount++;

        setState(DagState.INITIALIZING);
        raiseEventOnEdgeAdded(dependOn, dagNode);
    }

    public <P> void setInput(P param) {
        DagNode<? extends NodeBean<?>> root = getRootDagNode();
        if (null == root) {
            throw new IllegalStateException(String.format("Graph can not schedule without root node, %s", this));
        }

        root.getBean().setParam(param);
    }

    public R getOutput() {
        return (R) getFinalDagNode().getBean().getResult();
    }

    public boolean remove(DagNode<? extends NodeBean<?>> dagNode) {
        if (!dagNodes.contains(dagNode)) {
            return false;
        }
        synchronized (depends) {
            depends.remove(dagNode);
            for (Set<DagNode<?>> set : depends.values()) {
                set.remove(dagNode);
            }
        }

        synchronized (children) {
            children.remove(dagNode);
            for (Set<DagNode<?>> set : children.values()) {
                set.remove(dagNode);
            }
        }
        return true;
    }

    public boolean isScheduling() {
        return dagNodes.stream().anyMatch(n -> n.isScheduled() && !n.isFinished());
    }

    // 由 DAG调度器调用
    void setPrepared() {
        getDagNodes().forEach(t -> t.setStateChanged(this));
        setState(DagState.PREPARED);
    }

    // 由 DAG调度器调用
    void setScheduling() {
        setState(DagState.SCHEDULING);
    }

    // 由 DAG调度器调用
    void setCompleted() {
        setState(DagState.COMPLETED);
    }

    // 由 DAG调度器调用
    Set<DagNode<? extends NodeBean<?>>> getRootNodes() {
        return dagNodes.stream().filter(t -> t.getBean().isRoot()).collect(Collectors.toSet());
    }

    private void setState(DagState state) {
        if (null != this.state && this.state.equals(state)) {
            return;
        }
        DagState oldState = this.state;
        if (!transAllow(oldState, state)) {
            return;
        }
        this.state = state;

        raiseEventOnStateChanged(state);
    }

    private void raiseEventOnStateChanged(DagState state) {
        if (null != eventHandler) {
            switch (state) {
                case CREATED:
                    eventHandler.onCreated(this);
                    break;
                case PREPARED:
                    eventHandler.onPrepared(this);
                    break;
                case SCHEDULING:
                    eventHandler.onScheduling(this);
                    break;
                case COMPLETED:
                    eventHandler.onCompleted(this);
                    break;
            }
        }
    }

    private void raiseEventOnNodeAdded(DagNode<? extends NodeBean<?>> node) {
        if (null != eventHandler) {
            eventHandler.onNodeAdded(node, this);
        }
    }

    private void raiseEventOnEdgeAdded(DagNode<? extends NodeBean<?>> from, DagNode<? extends NodeBean<?>> to) {
        if (null != eventHandler) {
            eventHandler.onEdgeAdded(from, to, this);
        }
    }

    /**
     * 输出邻接矩阵
     */
    public Pair<DagNode<? extends NodeBean<?>>[], int[][]> buildAdjacencyMatrix() {
        DagNode<? extends NodeBean<?>>[] nodes = this.dagNodes.toArray(new DagNode<?>[0]);
        int size = nodes.length;
        int[][] matrix = new int[size][size];
        for (int i = 0; i < size; i++) {
            DagNode<?> node = nodes[i];
            for (int j = 0; j < nodes.length; j++) {
                boolean contains = children.getOrDefault(node, new HashSet<>()).contains(nodes[j]);
                matrix[i][j] = contains ? 1 : 0;
            }
        }

        return new ImmutablePair<>(nodes, matrix);
    }

    public String dumpAdjacencyMatrix() {
        Pair<DagNode<? extends NodeBean<?>>[], int[][]> pair = buildAdjacencyMatrix();
        StringBuilder builder = new StringBuilder();
        String title = "DAG GRAPH ADJACENCY MATRIX INFOS";
        title = Util.covering(title, title.length() + 10, "=", true);
        title = Util.covering(title, title.length() + 10, "=", false);
        int len = title.length();
        builder.append(title).append("\n");
        builder.append("nodes:\n");
        for (int i = 0; i < pair.getLeft().length; i++) {
            DagNode<? extends NodeBean<?>> node = pair.getLeft()[i];
            builder.append("(").append(i).append(")").append(node.getInfo().getName());
            if (i < pair.getLeft().length - 1) builder.append(", ");
        }
        builder.append("\n");
        builder.append(Util.repeat("-", len)).append("\n");
        builder.append("matrix:\n");
        int[][] matrix = pair.getRight();
        for (int i = 0; i < matrix.length; i++) {
            builder.append("(").append(i).append("): ").append(Arrays.toString(matrix[i])).append("\n");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return String.format("%s[graphId=%s, nodeCount=%s, edgeCount=%s]", this.getClass().getSimpleName(), graphId, dagNodes.size(), getEdgeCount());
    }

    @Override
    public void onCreated(Dag<?> graph) {
        log.trace("onDagCreated {}", graph);
    }

    @Override
    public void onPrepared(Dag<?> graph) {
        log.trace("onDagPrepared {}", graph);
    }

    @Override
    public void onScheduling(Dag<?> graph) {
        log.trace("onDagScheduling {}", graph);
    }

    @Override
    public void onCompleted(Dag<?> graph) {
        log.trace("onDagCompleted {}", graph);
    }

    @Override
    public void onNodeAdded(DagNode<? extends NodeBean<?>> node, Dag<?> graph) {
        log.trace("onDagNodeAdded: node={}, graph={}", node, graph);
    }

    @Override
    public void onEdgeAdded(DagNode<? extends NodeBean<?>> from, DagNode<? extends NodeBean<?>> to, Dag<?> graph) {
        log.trace("onDagEdgeAdded: from={}, to={}, graph={}", from, to, graph);
    }

    @Override
    public void onNodeStateChanged(DagNodeState oldState, DagNodeState newState, DagNodeInfo node) {
        log.trace("Node[{}] state changed: {} -> {}", node.getName(), oldState, newState);
    }

    private static String edgeName(@NonNull DagNode<? extends NodeBean<?>> from, @NonNull DagNode<? extends NodeBean<?>> to) {
        return String.format("%s->%s", from.getInfo().getName(), to.getInfo().getName());
    }
}
