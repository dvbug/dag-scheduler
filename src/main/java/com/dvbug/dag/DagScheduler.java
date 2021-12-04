package com.dvbug.dag;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DAG调度器
 */
@Slf4j
public class DagScheduler implements DagNodeStateChange {
    private static final ExecutorService pool = Executors.newFixedThreadPool(5);

    private final List<DagNode<?>> executedHistory = new ArrayList<>();

    public void schedule(Dag graph) {
        schedule(graph, false);
    }

    public void schedule(Dag graph, boolean snapshot) {
        graph.getDagNodes().forEach(t -> t.setStateChange(this));

        int times = 0;
        while (true) {
            List<DagNode<?>> todoNodes = new ArrayList<>();
            for (DagNode<?> dagNode : times == 0 ? graph.getRootNodes() : graph.getDagNodes()) {
                if (!dagNode.isScheduled()) {
                    Set<DagNode<? extends NodeBean>> depends = graph.getDepends().get(dagNode);
                    if (null == depends || depends.isEmpty() || depends.stream().allMatch(DagNode::isScheduled)) {
                        dagNode.setPrepared();
                        todoNodes.add(dagNode);
                    }
                }
            }

            if (!todoNodes.isEmpty()) {
                for (DagNode<? extends NodeBean> node : todoNodes) {
                    pool.submit(() -> scheduleNode(graph, snapshot, node));
                }
            } else {
                break;
            }
            times++;
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("Pool await error", e);
        }
    }

    private void scheduleNode(Dag graph, boolean snapshot, DagNode<? extends NodeBean> node) {
        Set<DagNode<? extends NodeBean>> children = graph.getChildren().get(node);
        boolean nodeExecSucceed = node.execute(new ExecuteCallback() {
            @Override
            public <R> void onCompleted(ExecuteResult<R> result) {
                log.debug("Node executed done, begin delivering result[{}] to {} children", result, children.size());
                if (!result.isSucceed()) {
                    for (DagNode<?> child : children) {
                        log.debug("Delivering node[{}] failure to child {}", result.getInfo().getName(), child);
                        child.setIneffective();
                        executedHistory.add(child);
                    }
                } else {
                    for (DagNode<?> child : children) {
                        log.debug("Delivering node[{}] result to child {}", result.getInfo().getName(), child);
                        child.putParam(result.isSucceed() ? result.getResult() : result.getThrowable());
                    }
                }
            }
        });
        if (snapshot) {
            log.info(dumpSnapshot(graph));
        }
        executedHistory.add(node);
        if (!nodeExecSucceed) {
            log.error("{} execute fail, {}", node, node.getNodeThrowable());
        }
    }

    public Object getResult(Dag graph) {
        return graph.getFinalDagNode().getBean().getResult();
    }

    public String dumpHistory(Dag graph) {
        return buildLog(String.format("%s HISTORY", graph.getClass().getSimpleName()), graph.getGraphId(), executedHistory, getResult(graph));
    }

    private String dumpSnapshot(Dag graph) {
        return buildLog(String.format("%s SNAPSHOT", graph.getClass().getSimpleName()), graph.getGraphId(), graph.getDagNodes(), getResult(graph));
    }

    private String buildLog(String marker, String graphId, Collection<DagNode<?>> nodes, Object result) {
        StringBuilder builder = new StringBuilder();
        String title = String.format("%s INFOS", marker);
        title = Util.covering(title, title.length() + 10, "=", true);
        title = Util.covering(title, title.length() + 10, "=", false);
        int len = title.length();
        builder.append(title).append("\n");
        builder.append("graphId=").append(graphId).append("\n");
        builder.append("nodes:\n");
        nodes.forEach(t -> {
            builder.append(t.toString());
            if (t.isFinished()) {
                builder.append(" result: ").append(t.getBean().getResult());
            }
            builder.append("\n");
        });
        builder.append(Util.repeat("=", len)).append("\n");
        builder.append("Result:\n");
        builder.append(result);
        return builder.toString();
    }

    @Override
    public void changed(DagNodeState oldState, DagNodeState newState, DagNodeInfo node) {
        log.trace("Node[{}] state changed: {} -> {}", node.getName(), oldState, newState);
    }
}
