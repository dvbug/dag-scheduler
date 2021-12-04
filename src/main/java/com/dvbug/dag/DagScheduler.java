package com.dvbug.dag;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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

    public void schedule(Dag DAG) {
        schedule(DAG, false);
    }

    public void schedule(Dag DAG, boolean snapshot) {
        DAG.getDagNodes().forEach(t -> t.setStateChange(this));

        int times = 0;
        while (true) {
            List<DagNode<?>> todoDagNodes = new ArrayList<>();
            for (DagNode<?> dagNode : times == 0 ? DAG.getRootNodes() : DAG.getDagNodes()) {
                if (!dagNode.isScheduled()) {
                    Set<DagNode<? extends NodeBean>> depends = DAG.getDepends().get(dagNode);
                    if (null != depends && !depends.isEmpty()) {
                        boolean canAdd = true;
                        for (DagNode<?> depT : depends) {
                            if (!depT.isScheduled()) {
                                canAdd = false;
                                break;
                            }
                        }
                        if (canAdd) {
                            dagNode.setPrepared();
                            todoDagNodes.add(dagNode);
                        }
                    } else {
                        dagNode.setPrepared();
                        todoDagNodes.add(dagNode);
                    }
                }
            }

            if (!todoDagNodes.isEmpty()) {
                for (DagNode<? extends NodeBean> dagNode : todoDagNodes) {
                    pool.submit(() -> {
                        Set<DagNode<? extends NodeBean>> children = DAG.getChildren().get(dagNode);
                        boolean nodeExecSucceed = dagNode.execute(new ExecuteCallback() {
                            @Override
                            public <R> void onCompleted(ExecuteResult<R> result) {
                                log.debug("Node executed done, begin delivering result[{}] to {} children", result, children.size());
                                if (!result.isSucceed()) {
                                    for (DagNode<?> child : children) {
                                        log.debug("Delivering node[{}] failure to child {}", result.getInfo().getName(), child);
                                        child.setIneffective();
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
                            log.info(dumpSnapshot(DAG));
                        }
                        executedHistory.add(dagNode);
                        if (!nodeExecSucceed) {
                            log.error("{} execute fail, {}", dagNode, dagNode.getNodeThrowable());
                        }
                    });
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

    public Object getResult(Dag dag) {
        return dag.getFinalDagNode().getBean().getResult();
    }

    public String dumpHistory(Dag DAG) {
        StringBuilder builder = new StringBuilder();
        String title = String.format("%s HISTORY INFOS", DAG.getClass().getSimpleName());
        title = Util.covering(title, title.length() + 10, "=", true);
        title = Util.covering(title, title.length() + 10, "=", false);
        int len = title.length();
        builder.append(title).append("\n");
        builder.append("graphId=").append(DAG.getGraphId()).append("\n");
        builder.append("nodes:\n");
        executedHistory.forEach(t -> {
            builder.append(t.toString()).append("\n");
        });
        builder.append(Util.repeat("=", len)).append("\n");
        builder.append("result:\n");
        builder.append(getResult(DAG));
        return builder.toString();
    }

    private String dumpSnapshot(Dag DAG) {
        StringBuilder builder = new StringBuilder();
        String title = String.format("%s SNAPSHOT INFOS", DAG.getClass().getSimpleName());
        title = Util.covering(title, title.length() + 10, "=", true);
        title = Util.covering(title, title.length() + 10, "=", false);
        int len = title.length();
        builder.append(title).append("\n");
        builder.append("graphId=").append(DAG.getGraphId()).append("\n");
        builder.append("nodes:\n");
        DAG.getDagNodes().forEach(t -> {
            builder.append(t.toString());
            if (t.isFinished()) {
                builder.append(" result: ").append(t.getBean().getResult());
            }
            builder.append("\n");
        });
        builder.append(Util.repeat("=", len)).append("\n");
        builder.append("Result:\n");
        builder.append(getResult(DAG));
        return builder.toString();
    }

    @Override
    public void changed(DagNodeState oldState, DagNodeState newState, DagNodeInfo node) {
        log.trace("Node[{}] state changed: {} -> {}", node.getName(), oldState, newState);
    }
}
