package com.dvbug.strategy;

/**
 * 策略类型
 */
public enum StrategyType {
    /**
     * 终点策略
     * 由 {@link FinalStrategy}实现
     */
    FINAL,
    /**
     * 逻辑策略
     * 由逻辑策略基础类{@link LogicStrategy}实现
     */
    LOGIC,

    /**
     * 起始策略
     * 由 {@link RootStrategy}实现
     */
    ROOT

}
