package com.sad.jetpack.v1.appgo.api;


import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

public interface IApplicationLifecyclesObserver {

    default public void onApplicationCreated(Application application){};

    default public void onApplicationLowMemory(Application application){};

    default public void onApplicationConfigurationChanged(Application application,Configuration newConfig){};

    default public void onApplicationTerminate(Application application){};

    default public void onApplicationTrimMemory(Application application,int level){};

    default public void attachApplicationBaseContext(Application application,Context base){};


    //Pre

    default public void onApplicationPreCreated(Application application){};

    default public void onApplicationPreLowMemory(Application application){};

    default public void onApplicationPreConfigurationChanged(Application application,Configuration newConfig){};

    default public void onApplicationPreTerminate(Application application){};

    default public void onApplicationPreTrimMemory(Application application,int level){};

    default public void attachApplicationPreBaseContext(Application application,Context base){};

}
