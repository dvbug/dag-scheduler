package com.dvbug.dag;

/**
 * DAG节点内连Bean对象
 */
public interface NodeBean {
    boolean isRoot();

    boolean isFinal();

    String getName();

    boolean execute();

    boolean executeAble();

    Object getResult();

    void setParam(Object param);

    int getParamCount();

    boolean equals(Object o);

    int hashCode();
}
