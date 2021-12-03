package com.dvbug.strategy;

import com.dvbug.strategy.StrategyBean;
import com.dvbug.strategy.StrategyType;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

public class StrategyDefinitions {

    @Slf4j
    @EqualsAndHashCode(callSuper = true)
    public static class RootStrategy extends StrategyBean<String> {
        public RootStrategy() {
            this("root");
        }

        public RootStrategy(String name) {
            super(name, StrategyType.ROOT);
            setParam(String.format("你好，这是一条由%s生成的测试语句", RootStrategy.class.getSimpleName()));
        }

        @Override
        public boolean doExecute() {
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
    public static class StringStrategy extends StrategyBean<String> {
        public StringStrategy(String name) {
            super(name, StrategyType.MIDDLE);
        }

        @Override
        public boolean doExecute() {
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
    public static class DoubleStrategy extends StrategyBean<Double> {
        public DoubleStrategy(String name) {
            super(name, StrategyType.MIDDLE);
        }

        @Override
        public boolean doExecute() {
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
    public static class FinalStrategy extends StrategyBean<Object> {
        public FinalStrategy() {
            super("final", StrategyType.FINAL);
        }

        @Override
        public boolean doExecute() {
            setResult(getParams().get(0));
            return true;
        }

        @Override
        public boolean canExecute() {
            return true;
        }
    }
}
