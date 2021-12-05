package com.dvbug.dag;

import com.dvbug.strategy.FinalStrategy;
import com.dvbug.strategy.RootStrategy;
import com.dvbug.strategy.StrategyDefinitions.DoubleStrategy;
import com.dvbug.strategy.StrategyDefinitions.StringStrategy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class MainTest {
    @Test
    public void testDAG() {
        Dag<String> graph = new Dag<>(DagMode.SWITCH, String.class, String.class);

        StringStrategy s1 = new StringStrategy("s1");
        StringStrategy s2 = new StringStrategy("s2");
        StringStrategy s3 = new StringStrategy("s3");
        StringStrategy s4 = new StringStrategy("s4");
        StringStrategy s5 = new StringStrategy("s5");
        StringStrategy s6 = new StringStrategy("s6");
        DoubleStrategy i1 = new DoubleStrategy("i1");
        DoubleStrategy i2 = new DoubleStrategy("i2");

        s1.setMockThrowable();
        i2.setMockThrowable();

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

        DagScheduler dagScheduler = new DagScheduler();
        dagScheduler.schedule(graph, "你好，这是一条由RootStrategy生成的测试语句", false);
        log.info(dagScheduler.dumpHistory(graph));
        log.info(graph.dumpAdjacencyMatrix());
        String output = graph.getOutput();
        log.info(output);
    }
}
