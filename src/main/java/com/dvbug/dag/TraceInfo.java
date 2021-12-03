package com.dvbug.dag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
public class TraceInfo {
    private long createdTime;
    private long startedTime;
    private long runningTime;
    private long completedTime;
    private boolean completed;
    private DagNodeState finalState = DagNodeState.CREATED;

    public TraceInfo(long createdTime) {
        this.createdTime = createdTime;
    }

    @Override
    public String toString() {
        return String.format("%s[createdT=%s,startedT=%s,runningT=%s,completedT=%s,completed=%s,state=%s]",
                TraceInfo.class.getSimpleName(),
                createdTime, startedTime, runningTime, completedTime, completed, finalState.toString());
    }
}
