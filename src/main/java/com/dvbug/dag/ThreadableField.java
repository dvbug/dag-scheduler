package com.dvbug.dag;

public class ThreadableField<T> implements RuntimeInitializable {
    private ThreadLocal<T> threadLocalField;
    private T mainThreadField;

    public ThreadableField() {
    }

    public ThreadableField(T value) {
        this.mainThreadField = value;
    }

    public synchronized T get() {
        if (null != threadLocalField) {
            if (null == threadLocalField.get() && null != mainThreadField) {
                this.threadLocalField.set(mainThreadField);
            }
            return threadLocalField.get();
        } else return mainThreadField;
    }

    public synchronized void set(T value) {
        if (null != threadLocalField) {
            threadLocalField.set(value);
        } else mainThreadField = value;
    }

    public synchronized void reset() {
        if (null != threadLocalField) {
            threadLocalField.remove();
        }
        threadLocalField = null;
        mainThreadField = null;
    }

    @Override
    public synchronized void beforeRuntime() {
        this.threadLocalField = new ThreadLocal<>();
        if (null != mainThreadField) {
            this.threadLocalField.set(mainThreadField);
        }
    }

    @Override
    public synchronized void afterRuntime() {
        if (null != threadLocalField) {
            threadLocalField.remove();
        }
        this.threadLocalField = null;
    }
}
