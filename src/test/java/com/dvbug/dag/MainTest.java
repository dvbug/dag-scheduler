package com.dvbug.dag;

import com.dvbug.strategy.FinalStrategy;
import com.dvbug.strategy.RootStrategy;
import com.dvbug.strategy.StrategyDefinitions.StringStrategy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MainTest {
    static ExecutorService pool = Executors.newFixedThreadPool(24);
    static DagScheduler dagScheduler = new DagScheduler();
    static Dag<String> graph = new Dag<>(DagMode.PARALLEL, -1, String.class, String.class);
    static boolean throwErr = false;


    @BeforeAll
    public static void initGraph() {
        StringStrategy s1 = new StringStrategy("s1");
        StringStrategy s2 = new StringStrategy("s2");
        StringStrategy s3 = new StringStrategy("s3");
        StringStrategy s4 = new StringStrategy("s4");
        StringStrategy s5 = new StringStrategy("s5");
        StringStrategy s6 = new StringStrategy("s6");
        StringStrategy i1 = new StringStrategy("i1");
        StringStrategy i2 = new StringStrategy("i2");

        if (throwErr) {
            s1.setMockThrowable();
            i2.setMockThrowable();
        }

        graph.addNode(s1);
        graph.addNode(s2);
        graph.addNode(s3);
        graph.addNode(s4);
        graph.addNode(s5);
        graph.addNode(s6);
        graph.addNode(i1);
        graph.addNode(i2);

        graph.addEdge("s1", RootStrategy.NAME);
        graph.addEdge("i1", RootStrategy.NAME);
        graph.addEdge("s2", "s1");
        graph.addEdge("s3", "s1");
        graph.addEdge("s4", "i1");
        graph.addEdge("i2", "i1");
        graph.addEdge("s5", "s3");
        graph.addEdge("s5", "s4");
        graph.addEdge("s6", "s2");
        graph.addEdge("s6", "s5");
        graph.addEdge("s6", "i2");
        graph.addEdge(FinalStrategy.NAME, "s6");
    }

    @Test
    public void testDAG() {
        int ROUND = 10;

        for (int i = 0; i < ROUND; i++) {
            DagResult<String> result = dagScheduler.schedule(graph, "测试语句" + (i + 1));
            if (ROUND > 1) {
                log.warn("{}, {}", result.getTraceId(), result.getResult());
            } else {
                log.info("{}", dagScheduler.printDagResult(result));
            }
        }
    }

    @Test
    public void testMultiThread() {
        int ROUND = 2;

        for (int i = 0; i < ROUND; i++) {
            final int N = i + 1;
            pool.submit(() -> {
                DagResult<String> result = dagScheduler.schedule(graph, "测试语句" + N);
                if (ROUND > 1) {
                    log.warn("{}, {}", result.getTraceId(), dagScheduler.printDagResult(result));
                } else {
                    log.info("{}", dagScheduler.printDagResult(result));
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("Test thread pool await error", e);
        }
    }
}
