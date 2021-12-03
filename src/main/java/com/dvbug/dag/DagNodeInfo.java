package com.dvbug.dag;

import lombok.*;

import java.util.Objects;

/**
 * DAG节点基础信息
 */
@Getter
@Setter(AccessLevel.PACKAGE)
@AllArgsConstructor(access=AccessLevel.MODULE)
public class DagNodeInfo {
    private final String id;
    private final String name;
    private final long timeout;


    public String toShortString() {
        return String.format("%s:%s", name, id);
    }

    public String toLongString() {
        return String.format("%s[%s:%s,timeout=%s]", DagNodeInfo.class.getSimpleName(), name, id, timeout);
    }

    @Override
    public String toString() {
        return toShortString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DagNodeInfo dagNodeInfo = (DagNodeInfo) o;
        return id.equals(dagNodeInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
