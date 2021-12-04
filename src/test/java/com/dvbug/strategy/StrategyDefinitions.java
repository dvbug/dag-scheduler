package com.dvbug.strategy;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

public class StrategyDefinitions {

    public static abstract class DebugStrategyBean<R> extends StrategyBean<R> {

        public DebugStrategyBean(String name, StrategyType type) {
            super(name, type);
        }

        public void setMockThrowable() {
            setThrowable(new RuntimeException("mock error for debug"));
        }

        @SneakyThrows
        @Override
        public boolean doExecute() {
            if (null != getThrowable()) {
                throw getThrowable();
            }
            return true;
        }
    }

    @Slf4j
    @EqualsAndHashCode(callSuper = true)
    public static class RootStrategy extends DebugStrategyBean<String> {
        public RootStrategy() {
            this("root");
        }

        public RootStrategy(String name) {
            super(name, StrategyType.ROOT);
            setParam(String.format("你好，这是一条由%s生成的测试语句", RootStrategy.class.getSimpleName()));
        }

        @Override
        public boolean doExecute() {
            super.doExecute();
            setResult(getParams().get(0).toString());
            return true;
        }

        @Override
        public boolean canExecute() {
            return true;
        }
    }

    @Slf4j
    @EqualsAndHashCode(callSuper = true)
    public static class StringStrategy extends DebugStrategyBean<String> {
        public StringStrategy(String name) {
            super(name, StrategyType.LOGIC);
        }

        @Override
        public boolean doExecute() {
            super.doExecute();
            // String changedData = Alg.randomString(3);
            String changedData = getName();
            String result = getParams().stream().map(p -> String.format("%s+%s", p, changedData)).collect(Collectors.joining(";"));

            setResult(result);
            return true;
        }

        @Override
        public boolean canExecute() {
            return true;
        }
    }

    @Slf4j
    @EqualsAndHashCode(callSuper = true)
    public static class DoubleStrategy extends DebugStrategyBean<Double> {
        public DoubleStrategy(String name) {
            super(name, StrategyType.LOGIC);
        }

        @Override
        public boolean doExecute() {
            super.doExecute();
            double sum = getParams().stream().map(p -> {
                double r = 0;
                if (p instanceof Number) {
                    r = ((double) p);
                } else {
                    r = p.toString().hashCode();
                }
                return r;
            }).mapToDouble(r -> r).sum();

            double result = Math.sqrt(Math.abs(sum));
            setResult(result);
            return true;
        }

        @Override
        public boolean canExecute() {
            return true;
        }
    }

    @Slf4j
    @EqualsAndHashCode(callSuper = true)
    public static class FinalStrategy extends DebugStrategyBean<Object> {
        public FinalStrategy() {
            super("final", StrategyType.FINAL);
        }

        @Override
        public boolean doExecute() {
            super.doExecute();
            setResult(getParams().get(0));
            return true;
        }

        @Override
        public boolean canExecute() {
            return true;
        }
    }
}
