package com.dvbug.dag;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Dag {
    private final DagMode mode;
    private final String graphId;
    private int edgeCount = 0;
    private DagNode<? extends NodeBean> finalDagNode;
    private final Set<DagNode<? extends NodeBean>> dagNodes;
    private final Map<DagNode<? extends NodeBean>, Set<DagNode<? extends NodeBean>>> depends;
    private final Map<DagNode<? extends NodeBean>, Set<DagNode<? extends NodeBean>>> children;

    public Dag(DagMode mode) {
        this(mode, UUID.randomUUID().toString().replaceAll("-", ""));
    }

    public Dag(DagMode mode, String graphId) {
        this.mode = mode;
        this.graphId = graphId;
        this.dagNodes = new HashSet<>();
        this.depends = new HashMap<>();
        this.children = new HashMap<>();
    }

    public void addEdge(DagNode<? extends NodeBean> dagNode, DagNode<? extends NodeBean> dependOn) {
        String edgeName = edgeName(dependOn, dagNode);

        if (!dagNodes.contains(dagNode) || !dagNodes.contains(dependOn)) {
            throw new IllegalStateException(String.format("Dag graph edge[%s] can not independent", edgeName));
        }

        Set<DagNode<? extends NodeBean>> depends = this.depends.computeIfAbsent(dagNode, k -> new HashSet<>());
        Set<DagNode<? extends NodeBean>> children = this.children.computeIfAbsent(dependOn, k -> new HashSet<>());

        if (depends.contains(dependOn)) {
            throw new IllegalStateException(String.format("Dag graph edge[%s] is existed", edgeName));
        }

        depends.add(dependOn);
        dagNode.setExpectDependCount(depends.size());
        children.add(dagNode);
        edgeCount++;
    }

    public void addNode(DagNode<? extends NodeBean> dagNode) {
        dagNode.setMode(this.mode);

        if (dagNodes.contains(dagNode)) {
            throw new IllegalArgumentException(String.format("%s is existed", dagNode));
        }
        if (dagNode.getBean().isRoot() && dagNodes.stream().anyMatch(t -> t.getBean().isRoot())) {
            throw new IllegalArgumentException("Just set root once");
        }

        if (dagNode.getBean().isFinal() && dagNodes.stream().anyMatch(t -> t.getBean().isFinal())) {
            throw new IllegalArgumentException("Just set final once");
        }
        dagNodes.add(dagNode);
        if (dagNode.getBean().isFinal()) {
            finalDagNode = dagNode;
        }
    }

    public boolean remove(DagNode<? extends NodeBean> dagNode) {
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

    public Set<DagNode<? extends NodeBean>> getRootNodes() {
        return dagNodes.stream().filter(t -> t.getBean().isRoot()).collect(Collectors.toSet());
    }

    /**
     * 输出邻接矩阵
     */
    public Pair<DagNode<? extends NodeBean>[], int[][]> buildAdjacencyMatrix() {
        DagNode<? extends NodeBean>[] nodes = this.dagNodes.toArray(new DagNode<?>[0]);
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
        Pair<DagNode<? extends NodeBean>[], int[][]> pair = buildAdjacencyMatrix();
        StringBuilder builder = new StringBuilder();
        String title = "DAG GRAPH ADJACENCY MATRIX INFOS";
        title = Util.covering(title, title.length() + 10, "=", true);
        title = Util.covering(title, title.length() + 10, "=", false);
        int len = title.length();
        builder.append(title).append("\n");
        builder.append("nodes:\n");
        for (int i = 0; i < pair.getLeft().length; i++) {
            DagNode<? extends NodeBean> node = pair.getLeft()[i];
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

    private static String edgeName(@NonNull DagNode<? extends NodeBean> start, @NonNull DagNode<? extends NodeBean> end) {
        return String.format("%s->%s", start.getInfo().toShortString(), end.getInfo().toShortString());
    }
}
