package com.dvbug.dag;

import java.util.*;

import static com.dvbug.dag.DagNodeState.*;

/**
 * 状态机有序判断
 * 标记某个次态可由哪些现态转化而来
 */
public class DagNodeStateTransition {
    public static final Map<DagNodeState, DagNodeState[]> ALLOWED_TRANSFERS = new HashMap<DagNodeState, DagNodeState[]>() {{
        put(CREATED, new DagNodeState[0]);
        put(PREPARED, new DagNodeState[]{CREATED});
        put(START, new DagNodeState[]{PREPARED});
        put(WAITING, new DagNodeState[]{START});
        put(RUNNING, new DagNodeState[]{START, WAITING});
        put(SUCCESS, new DagNodeState[]{RUNNING});
        put(FAILED, new DagNodeState[]{CREATED, PREPARED, START, WAITING, RUNNING});
        put(INEFFECTIVE, new DagNodeState[]{PREPARED, START, WAITING, RUNNING});
    }};

    /**
     * 状态改变合法性检测
     *
     * @param oldState 现态
     * @param newState 次态
     * @return 是否可以转化
     */
    public static boolean transAllow(DagNodeState oldState, DagNodeState newState) {
        return !ALLOWED_TRANSFERS.containsKey(newState) ||
                ALLOWED_TRANSFERS.get(newState).length == 0 ||
                Arrays.stream(ALLOWED_TRANSFERS.get(newState)).anyMatch(t -> t == oldState);
    }
}
