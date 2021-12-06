package com.dvbug.dag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TraceInfo {
    @Setter(AccessLevel.MODULE)
    @Getter
    private String id;
    @Getter
    private final DagNodeInfo nodeInfo;
    private final Map<DagNodeState, Long> stateChangedTimeMap = new HashMap<>();
    @Getter
    private DagNodeState finalState;
    @Getter
    private final List<DagNode<? extends NodeBean<?>>> failedDepends = new ArrayList<>();
    @Setter(AccessLevel.MODULE)
    @Getter
    private Object finalResult;

    TraceInfo(DagNodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
        for (DagNodeState state : DagNodeState.values()) {
            stateChangedTimeMap.put(state, 0L);
        }
    }

    public void setFinalState(DagNodeState state) {
        this.finalState = state;
        stateChangedTimeMap.replace(state, System.currentTimeMillis());
    }

    public boolean isCompleted() {
        return DagNodeStateTransition.isFinalState(finalState);
    }

    public long getStateTime(DagNodeState state) {
        return stateChangedTimeMap.get(state);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getSimpleName()).append("[");
        builder.append("id=").append(id).append(",");
        builder.append("nodeInfo=").append(nodeInfo).append(",");
        stateChangedTimeMap.forEach(((state, ts) -> {
            builder.append(state.toString().toLowerCase()).append("T=").append(ts).append(",");
        }));
        builder.append("state=").append(finalState.toString()).append(",");
        builder.append("failedDepends=[").append(failedDepends.stream().map(n -> n.getInfo().getName()).collect(Collectors.joining(","))).append("],");
        builder.append("result=").append(finalResult);
        builder.append("]");
        return builder.toString();
    }
}
