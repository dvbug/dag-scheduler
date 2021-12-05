package com.dvbug.strategy;

import lombok.*;

/**
 * 起始策略, 每个策略图中必须由且只有1个
 * 可由策略图自行创建
 */
@EqualsAndHashCode(callSuper = true)
public final class RootStrategy extends StrategyBean<Object> {
    public static final String NAME = "root";

    @Getter(AccessLevel.PRIVATE)
    private Class<?> inputType;

    public RootStrategy(Class<?> inputType) {
        this(NAME, StrategyType.ROOT);
        this.inputType = inputType;
    }

    RootStrategy(String name, StrategyType type) {
        super(name, type);
    }

    @Override
    public void setParam(@NonNull Object param) {
        if (!param.getClass().isAssignableFrom(getInputType())) {
            throw new IllegalStateException(String.format("RootStrategy param[%s] type must %s", param, getInputType()));
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
