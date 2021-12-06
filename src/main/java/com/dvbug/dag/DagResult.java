package com.dvbug.dag;

import lombok.Data;

import java.util.List;

@Data
public class DagResult<R> {
    private final String graphId;
    private final String traceId;
    private final DagMode graphMode;
    private final R result;
    private final List<TraceInfo> history;

    DagResult(String graphId, String traceId, DagMode graphMode, List<TraceInfo> history, R result) {
        this.graphId = graphId;
        this.traceId = traceId;
        this.graphMode = graphMode;
        this.history = history;
        this.result = result;
    }
}
