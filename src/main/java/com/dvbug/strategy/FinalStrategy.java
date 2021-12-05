package com.dvbug.strategy;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 终点策略, 每个策略图中必须由且只有1个
 * 可由策略图自行创建
 */
@EqualsAndHashCode(callSuper = true)
public final class FinalStrategy extends StrategyBean<Object> {
    public static final String NAME = "final";

    @Getter(AccessLevel.PRIVATE)
    private Class<?> resultType;

    public FinalStrategy(Class<?> resultType) {
        this(NAME, StrategyType.FINAL);
        this.resultType = resultType;
    }

    FinalStrategy(String name, StrategyType type) {
        super(name, type);
    }

    @Override
    public void setParam(Object param) {
        if (!param.getClass().isAssignableFrom(getResultType())) {
            throw new IllegalStateException(String.format("FinalStrategy param[%s] type must %s", param, getResultType()));
        }
        super.setParam(param);
    }

    @Override
    public boolean doExecute() {
        setResult(getParams().get(0));
        return true;
    }

    @Override
    public boolean canExecute() {
        return !getParams().isEmpty();
    }
}
