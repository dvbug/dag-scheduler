package com.dvbug.dag;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * DAG节点基础信息
 */
@Getter
@Setter(AccessLevel.PACKAGE)
public class DagNodeInfo {
    private String graphId;
    private DagMode mode;
    private long timeout;
    private final String name;
    private final boolean isRoot;
    private final boolean isFinal;

    public DagNodeInfo(String name, long timeout, boolean isRoot, boolean isFinal) {
        this.name = name;
        this.timeout = timeout;
        this.isRoot = isRoot;
        this.isFinal = isFinal;
    }

    @Override
    public String toString() {
        return String.format("%s[%s:%s,timeout=%s,isRoot=%s,isFinal=%s]", DagNodeInfo.class.getSimpleName(), graphId, name, timeout, isRoot, isFinal);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DagNodeInfo that = (DagNodeInfo) o;
        return graphId.equals(that.graphId) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphId, name);
    }
}
