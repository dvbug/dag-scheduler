package com.dvbug.dag;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * {@link Dag<>}调度器
 */
@Slf4j
public final class DagScheduler {
    private static final ExecutorService pool = Executors.newFixedThreadPool(24);

    public <P, R> DagResult<R> schedule(Dag<R> graph, P inputParam) {
        String traceId = UUID.randomUUID().toString().toLowerCase().replaceAll("-", "");

        log.info("{}[{}] start with input={} to graph={}", traceId, this.getClass().getSimpleName(), inputParam, graph);

        if (graph.isScheduling()) {
            log.info(graph.dumpSnapshot());
            throw new IllegalStateException(String.format("%s is scheduling now", graph));
        }

        List<TraceInfo> history = new ArrayList<>();
        List<Future<TraceInfo>> cache = new ArrayList<>();

        graph.setPrepared(traceId);
        graph.setInput(inputParam);

        int times = 0;
        while (true) {
            List<DagNode<?>> todoNodes = new ArrayList<>();
            for (DagNode<?> dagNode : times == 0 ? graph.getRootNodes() : graph.getDagNodes()) {
                if (!dagNode.isScheduled()) {
                    Set<DagNode<? extends NodeBean<?>>> depends = graph.getDepends().get(dagNode);
                    if (null == depends || depends.isEmpty() || depends.stream().allMatch(DagNode::isScheduled)) {
                        dagNode.setPrepared();
                        todoNodes.add(dagNode);
                    }
                }
            }

            if (!todoNodes.isEmpty()) {
                graph.setScheduling();
                for (DagNode<? extends NodeBean<?>> node : todoNodes) {
                    Future<TraceInfo> future = pool.submit(() -> scheduleNode(graph, node));
                    cache.add(future);
                }
            } else {
                break;
            }
            times++;
        }

        //pool.shutdown();
        long timeout = graph.getTimeout() < 0 ? 2000 : graph.getTimeout() + 500;
        for (Future<TraceInfo> future : cache) {
            try {
                history.add(future.get(timeout, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                log.warn("Scheduler thread pool interrupted error", e);
            } catch (ExecutionException e) {
                log.error("Scheduler thread pool execution error, future: {}, {}", future, e);
            } catch (TimeoutException e) {
                log.error("Scheduler thread pool timeout error", e);
            }
        }

        log.info("{} done, graph={}, result={}", this.getClass().getSimpleName(), graph, graph.getOutput());

        try {
            return new DagResult<>(graph.getGraphId(), traceId, graph.getMode(), history, graph.getOutput());
        } finally {
            graph.setCompleted();
        }
    }

    private TraceInfo scheduleNode(Dag<?> graph, DagNode<? extends NodeBean<?>> node) {
        node.beforeRuntime();
        Set<DagNode<? extends NodeBean<?>>> children = graph.getChildren().getOrDefault(node, new HashSet<>());
        boolean nodeExecSucceed = node.execute(new DagNodeExecutionCallback() {
            @Override
            public <R> void onCompleted(DagNodeExecuteResult<R> result) {
                log.debug("Node[{}] executed done, begin delivering execute result [{}] to {} children", node.getInfo().getName(), result, children.size());
                if (!result.isSucceed()) {
                    node.getTrace().setFinalResult(result.getThrowable());
                    for (DagNode<?> child : children) {
                        log.debug("Delivering node[{}] failure to child {}", result.getInfo().getName(), child);
                        child.notifyDependFail(node);
                    }
                } else {
                    node.getTrace().setFinalResult(result.getResult());
                    for (DagNode<?> child : children) {
                        log.debug("Delivering node[{}] result to child {}", result.getInfo().getName(), child);
                        child.putParam(result.getResult());
                    }
                }
            }
        });

        if (!nodeExecSucceed) {
            log.error("{} execute fail, trace={}, {}", node, node.getTrace(), node.getNodeThrowable());
        }

        try {
            return node.getTrace();
        } finally {
            node.afterRuntime();
        }
    }

    public String printDagResult(DagResult<?> dagResult) {
        StringBuilder builder = new StringBuilder();
        String title = String.format("%s HISTORY INFOS", dagResult.getClass().getSimpleName());
        title = Util.covering(title, title.length() + 10, "=", true);
        title = Util.covering(title, title.length() + 10, "=", false);
        int len = title.length();
        builder.append(title).append("\n");
        builder.append("traceId=").append(dagResult.getTraceId()).append("\n");
        builder.append("graphId=").append(dagResult.getGraphId()).append("\n");
        builder.append("mode=").append(dagResult.getGraphMode()).append("\n");
        builder.append(Util.repeat("-", len)).append("\n");
        builder.append("histories:\n");
        dagResult.getHistory().forEach(t -> {
            builder.append(String.format("Node[%s:%s] ", t.getNodeInfo().getName(), t.getFinalState()));
            builder.append(t).append("\n");
        });
        builder.append(Util.repeat("=", len)).append("\n");
        builder.append("result:\n");
        builder.append(dagResult.getResult());
        return builder.toString();
    }

    public void shutdown() {
        if (!pool.isShutdown()) {
            pool.shutdown();
        }
    }
}
