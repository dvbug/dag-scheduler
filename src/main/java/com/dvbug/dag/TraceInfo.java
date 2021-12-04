package com.dvbug.dag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
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

    public void setFinalState(DagNodeState finalState) {
        this.finalState = finalState;
        update();
    }

    private void update() {
        switch (finalState) {
            case START:
                setStartedTime(System.currentTimeMillis());
                break;
            case FAILED:
            case SUCCESS:
                setCompleted(true);
                setCompletedTime(System.currentTimeMillis());
                break;
            case RUNNING:
                setRunningTime(System.currentTimeMillis());
                break;
        }
    }
    @Override
    public String toString() {
        return String.format("%s[createdT=%s,startedT=%s,runningT=%s,completedT=%s,completed=%s,state=%s]",
                TraceInfo.class.getSimpleName(),
                createdTime, startedTime, runningTime, completedTime, completed, finalState.toString());
    }
}
