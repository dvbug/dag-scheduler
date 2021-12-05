package com.dvbug.strategy;

import lombok.EqualsAndHashCode;

/**
 * 逻辑策略基础类
 * 由具体业务继承实现
 * <p>
 * {@link LogicStrategy#executeAble()} 和 {@link LogicStrategy#doExecute()}方法
 * 具体参见{@link StrategyBean}基类介绍和{@link com.dvbug.dag.NodeBean<>}接口介绍
 *
 * @param <R> 策略输出结果类型
 */
@EqualsAndHashCode(callSuper = true)
public abstract class LogicStrategy<R> extends StrategyBean<R> {

    public LogicStrategy(String name) {
        this(name, StrategyType.LOGIC);
    }

    LogicStrategy(String name, StrategyType type) {
        super(name, type);
    }
}
