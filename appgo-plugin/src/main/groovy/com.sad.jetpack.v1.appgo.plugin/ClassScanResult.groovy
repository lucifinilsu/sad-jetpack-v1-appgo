package com.sad.jetpack.v1.appgo.plugin

import javassist.CtClass

class ClassScanResult{

    private File dest;
    private CtClass applicationClass;
    private boolean handled=false;
    private ArrayList<CtClass> lifecyclesObserverClassList=new ArrayList<>();

    ArrayList<CtClass> getLifecyclesObserverClassList() {
        return lifecyclesObserverClassList
    }

    File getDest() {
        return dest
    }

    void setDest(File dest) {
        this.dest = dest
    }

    CtClass getApplicationClass() {
        return applicationClass
    }

    void setApplicationClass(CtClass applicationClass) {
        this.applicationClass = applicationClass
    }

    boolean getHandled() {
        return handled
    }

    void setHandled(boolean handled) {
        this.handled = handled
    }
}