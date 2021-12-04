package com.dvbug.strategy;

import com.dvbug.dag.NodeBean;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 策略基础类
 * @param <R> 策略输出结果类型
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public abstract class StrategyBean<R> implements NodeBean {
    private final String name;
    private final StrategyType type;
    private R result;
    @Setter(AccessLevel.PROTECTED)
    private Throwable throwable;
    @Setter(AccessLevel.NONE)
    private List<Object> params = new ArrayList<>();
    private final Object lockObj = new Object();

    public final boolean execute() {
        synchronized (lockObj) {
            params = params.stream().sorted(Comparator.comparing(Objects::hashCode)).collect(Collectors.toList());
        }
        log.debug("{} environment: params={}", this, getParams());
        try {
            boolean ok = executeAble() && doExecute();
            if(ok) {
                setThrowable(null);
            }
            return ok;
        } catch (Throwable e) {
            setResult(null);
            setThrowable(e);
            return false;
        }finally {
            if(null == throwable) {
                log.debug("{} do {} to: {}", this, getParams(), getResult());
            } else {
                log.warn("{} do {} to: {}, err: {}", this, getParams(), getResult(), getThrowable());
            }
        }
    }

    public final boolean executeAble() {
        return (!params.isEmpty()) && canExecute();
    }

    /**
     * 核心执行动作方法<br/>
     *
     * 子类如果执行成功,需要调用 {@link StrategyBean#setResult(R)} 设置执行结果<br/>
     * 子类如果执行失败,需要调用 {@link StrategyBean#setThrowable(Throwable)} 设置执行异常<br/>
     * 子类如果抛出异常, {@link StrategyBean#execute()}<br/>
     * 内会自动调用 {@link StrategyBean#setThrowable(Throwable)} 设置执行异常,并返回 false<br/>
     * @return 返回是否执行成功
     */
    public abstract boolean doExecute();

    public abstract boolean canExecute();

    @Override
    public void setParam(Object param) {
        this.params.add(param);
    }

    @Override
    public boolean isFinal() {
        return type == StrategyType.FINAL;
    }

    @Override
    public boolean isRoot() {
        return type == StrategyType.ROOT;
    }

    @Override
    public int getParamCount() {
        return this.params.size();
    }

    @Override
    public String toString() {
        return String.format("%s[%s:%s]", this.getClass().getSimpleName(), name, type);
    }
}
