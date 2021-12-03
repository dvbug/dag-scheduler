package com.dvbug.dag;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import com.dvbug.strategy.StrategyDefinitions.*;

@Slf4j
public class MainTest{
    @Test
    public void testDAG() {
        Dag graph = new Dag();
        DagNode<RootStrategy> rootS = new DagNode<>(new RootStrategy());
        DagNode<FinalStrategy> finalS = new DagNode<>(new FinalStrategy());

        DagNode<StringStrategy> s1 = new DagNode<>(new StringStrategy("s1"));
        DagNode<StringStrategy> s2 = new DagNode<>(new StringStrategy("s2"));
        DagNode<StringStrategy> s3 = new DagNode<>(new StringStrategy("s3"));
        DagNode<StringStrategy> s4 = new DagNode<>(new StringStrategy("s4"));
        DagNode<StringStrategy> s5 = new DagNode<>(new StringStrategy("s5"));
        DagNode<StringStrategy> s6 = new DagNode<>(new StringStrategy("s6"));
        DagNode<DoubleStrategy> i1 = new DagNode<>(new DoubleStrategy("i1"));
        DagNode<DoubleStrategy> i2 = new DagNode<>(new DoubleStrategy("i2"));

        graph.addNode(rootS);
        graph.addNode(finalS);
        graph.addNode(s1);
        graph.addNode(s2);
        graph.addNode(s3);
        graph.addNode(s4);
        graph.addNode(s5);
        graph.addNode(s6);
        graph.addNode(i1);
        graph.addNode(i2);

        graph.addEdge(s1, rootS);
        graph.addEdge(i1, rootS);
        graph.addEdge(s2, s1);
        graph.addEdge(s3, s1);
        graph.addEdge(s4, i1);
        graph.addEdge(i2, i1);
        graph.addEdge(s5, s3);
        graph.addEdge(s5, s4);
        graph.addEdge(s6, s2);
        graph.addEdge(s6, s5);
        graph.addEdge(s6, i2);
        graph.addEdge(finalS, s6);

        DagScheduler dagScheduler = new DagScheduler();
        dagScheduler.schedule(graph);

        log.info(dagScheduler.dumpHistory(graph));

    }
}
