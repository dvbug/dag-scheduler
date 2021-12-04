package com.dvbug.dag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * 现态将来是否可能转化为指定的状态
     *
     * @param current    现态
     * @param maybeState 待判断的状态
     * @return 是否可能转化
     */
    public static boolean maybeTransAllow(DagNodeState current, DagNodeState maybeState) {
        return Arrays.stream(ALLOWED_TRANSFERS.get(maybeState)).anyMatch(t -> t == current || maybeTransAllow(current, t));
    }

    /**
     * 判断指定状态是否为最终状态
     *
     * @param state 待判断的状态
     * @return 是否是最终状态
     */
    public static boolean isFinalState(DagNodeState state) {
        return Arrays.stream(values()).noneMatch(t -> maybeTransAllow(state, t));
    }
}
