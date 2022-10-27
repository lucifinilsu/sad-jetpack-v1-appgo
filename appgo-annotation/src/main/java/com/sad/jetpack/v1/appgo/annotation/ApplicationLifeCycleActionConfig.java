package com.sad.jetpack.v1.appgo.annotation;

public class ApplicationLifeCycleActionConfig {
    private String[] processName=new String[]{};
    private long priority;
    private String hostClassName = "";
    private String hostMethodName ="";

    public String getHostClassName() {
        return hostClassName;
    }

    public void setHostClassName(String hostClassName) {
        this.hostClassName = hostClassName;
    }

    public String getHostMethodName() {
        return hostMethodName;
    }

    public void setHostMethodName(String hostMethodName) {
        this.hostMethodName = hostMethodName;
    }

    public String[] getProcessName() {
        return processName;
    }

    public void setProcessName(String[] processName) {
        this.processName = processName;
    }

    public long getPriority() {
        return priority;
    }

    public void setPriority(long priority) {
        this.priority = priority;
    }
}
