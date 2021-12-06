package com.dvbug.strategy;

import com.dvbug.dag.NodeBean;
import com.dvbug.dag.ThreadableField;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 策略基础类
 *
 * @param <R> 策略输出结果类型
 */
@Slf4j
@RequiredArgsConstructor
public abstract class StrategyBean<R> implements NodeBean<R> {
    @Getter
    private final String name;
    @Getter
    private final StrategyType type;
    private final ThreadableField<R> result = new ThreadableField<>();
    private final ThreadableField<Boolean> isSetResult = new ThreadableField<>(false);
    private final ThreadableField<Boolean> isSetError = new ThreadableField<>(false);
    private final ThreadableField<Throwable> throwable = new ThreadableField<>();
    private final ThreadableField<List<Object>> params = new ThreadableField<>(new ArrayList<>());

    @Override
    public void beforeRuntime() {
        this.result.beforeRuntime();
        this.isSetResult.beforeRuntime();
        this.isSetError.beforeRuntime();
        this.throwable.beforeRuntime();
        this.params.beforeRuntime();
    }

    @Override
    public void afterRuntime() {
        this.result.afterRuntime();
        this.isSetResult.afterRuntime();
        this.isSetError.afterRuntime();
        this.throwable.afterRuntime();
        this.params.afterRuntime();
    }

    @Override
    public void reset() {
        this.result.reset();
        this.isSetResult.reset();
        this.isSetError.reset();
        this.throwable.reset();
        this.params.reset();

        this.isSetResult.set(false);
        this.isSetError.set(false);
        this.params.set(new ArrayList<>());
    }

    public final boolean execute() {
        params.set(params.get().stream().sorted(Comparator.comparing(Objects::hashCode)).collect(Collectors.toList()));

        log.debug("{} execute: params={}", this, getParams());
        try {
            boolean ok = executeEnable() && doExecute();
            if (ok) {
                if (isSetResult.get()) {
                    unsetThrowable();
                } else {
                    log.error("{}.doExecute() returns true, but without any result, please invoke setResult(result)", this.getClass().getSimpleName());
                    setThrowable(new IllegalStateException(String.format("%s result is not set", this.getClass().getSimpleName())));
                    ok = false;
                }
            } else {
                unsetResult();
                if (!isSetError.get()) {
                    setThrowable(new IllegalStateException(String.format("%s throwable is not set", this.getClass().getSimpleName())));
                }
            }
            return ok;
        } catch (Throwable e) {
            unsetResult();
            setThrowable(e);
            return false;
        } finally {
            if (null == throwable.get()) {
                log.debug("{} do {} to: {}", this, getParams(), getResult());
            } else {
                log.warn("{} do {} err: {}", this, getParams(), getThrowable());
            }
        }
    }

    /**
     * 业务方关注<br/>
     * 业务方必须在{@link NodeBean#execute()}方法返回true前调用
     *
     * @param result {@link R}的实例
     */
    public void setResult(R result) {
        this.result.set(result);
        this.isSetResult.set(true);
    }

    /**
     * 业务方关注<br/>
     * 业务方必须在{@link NodeBean#execute()}方法返回false前调用
     *
     * @param throwable {@link Throwable}的实例
     */
    public void setThrowable(Throwable throwable) {
        this.throwable.set(throwable);
        this.isSetError.set(true);
    }

    public final boolean executeEnable() {
        return (!params.get().isEmpty()) && canExecute();
    }

    /**
     * 核心执行动作方法<br/>
     * <p>
     * 子类如果执行成功,需要调用 {@link StrategyBean#setResult(R)} 设置执行结果,
     * 否则{@link StrategyBean#execute()}将自动补充异常并强制返回false<br/>
     * 子类如果执行失败,需要调用 {@link StrategyBean#setThrowable(Throwable)} 设置执行异常,
     * 否则{@link StrategyBean#execute()}将自动补充异常并强制返回false<br/>
     * <p>
     * 子类如果抛出异常, {@link StrategyBean#execute()}内会自动调用
     * {@link StrategyBean#setThrowable(Throwable)}设置执行异常,并强制返回false<br/>
     *
     * @return 返回是否执行成功
     */
    public abstract boolean doExecute();

    /**
     * 业务方关注<br/>
     * {@link StrategyBean#doExecute()}方法被执行的前置条件<br/>
     * 通常在本方法内部进行可执行的参数和条件是否满足<br/>
     */
    public abstract boolean canExecute();

    public void onBeforeRuntime() {
    }

    public void onAfterRuntime() {
    }

    @Override
    public void setParam(Object param) {
        this.params.get().add(param);
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
        return this.params.get().size();
    }

    public List<Object> getParams() {
        return params.get();
    }

    @Override
    public R getResult() {
        return result.get();
    }

    @Override
    public Throwable getThrowable() {
        return throwable.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyBean<?> that = (StrategyBean<?>) o;
        return name.equals(that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return String.format("%s[%s:%s]", this.getClass().getSimpleName(), name, type);
    }

    private void unsetResult() {
        this.isSetResult.reset();
        this.result.reset();
    }

    private void unsetThrowable() {
        this.isSetError.reset();
        this.throwable.reset();
    }
}
