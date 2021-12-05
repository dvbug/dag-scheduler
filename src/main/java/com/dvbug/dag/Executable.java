package com.dvbug.dag;

public interface Executable {
    boolean execute(DagNodeExecutionCallback callback);
}
