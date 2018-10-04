package com.keith.wechat.monitor.utility;

public abstract class RunnableWithArgs implements Runnable {
    public void setArgs(Object... args) { }
    public Object getResult() {
        return null;
    }
}