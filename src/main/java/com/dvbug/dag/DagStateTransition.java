package com.dvbug.dag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.dvbug.dag.DagState.*;


/**
 * {@link DagState}状态机
 */
public class DagStateTransition {
    public static final Map<DagState, DagState[]> ALLOWED_TRANSFERS = new HashMap<DagState, DagState[]>() {{
        put(CREATED, new DagState[0]);
        put(INITIALIZING, new DagState[]{CREATED});
        put(PREPARED, new DagState[]{INITIALIZING});
        put(SCHEDULING, new DagState[]{PREPARED});
        put(COMPLETED, new DagState[]{SCHEDULING});
    }};

    /**
     * 状态改变合法性检测
     *
     * @param oldState 现态
     * @param newState 次态
     * @return 是否可以转化
     */
    public static boolean transAllow(DagState oldState, DagState newState) {
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
    public static boolean maybeTransAllow(DagState current, DagState maybeState) {
        return Arrays.stream(ALLOWED_TRANSFERS.get(maybeState)).anyMatch(t -> t == current || maybeTransAllow(current, t));
    }

    /**
     * 判断指定状态是否为最终状态
     *
     * @param state 待判断的状态
     * @return 是否是最终状态
     */
    public static boolean isFinalState(DagState state) {
        return Arrays.stream(values()).noneMatch(t -> maybeTransAllow(state, t));
    }
}
