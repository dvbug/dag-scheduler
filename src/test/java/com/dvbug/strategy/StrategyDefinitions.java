package com.dvbug.strategy;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

public class StrategyDefinitions {

    public static abstract class DebugLogicStrategy<R> extends LogicStrategy<R> {
        public DebugLogicStrategy(String name) {
            super(name);
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
    public static class StringStrategy extends DebugLogicStrategy<String> {

        public StringStrategy(String name) {
            super(name);
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
    public static class DoubleStrategy extends DebugLogicStrategy<Double> {

        public DoubleStrategy(String name) {
            super(name);
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
}
