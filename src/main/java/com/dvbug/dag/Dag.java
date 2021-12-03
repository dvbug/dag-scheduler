package com.dvbug.dag;

import lombok.Getter;
import lombok.NonNull;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Dag {
    private final String graphId;
    private DagNode<? extends NodeBean> finalDagNode;
    private final Set<DagNode<? extends NodeBean>> dagNodes;
    private final Map<DagNode<? extends NodeBean>, Set<DagNode<? extends NodeBean>>> depends;
    private final Map<DagNode<? extends NodeBean>, Set<DagNode<? extends NodeBean>>> children;

    public Dag() {
        this(UUID.randomUUID().toString().replaceAll("-", ""));
    }

    public Dag(String graphId) {
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
    }

    public void addNode(DagNode<? extends NodeBean> dagNode) {
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

    private static String edgeName(@NonNull DagNode<? extends NodeBean> start, @NonNull DagNode<? extends NodeBean> end) {
        return String.format("%s->%s", start.getInfo().toShortString(), end.getInfo().toShortString());
    }
}
