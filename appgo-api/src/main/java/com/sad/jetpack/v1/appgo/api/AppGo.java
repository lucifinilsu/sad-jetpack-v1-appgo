package com.sad.jetpack.v1.appgo.api;

import android.app.Application;
import android.content.Context;

public class AppGo {

    public static boolean init(Context context){
        if (getContext()==null){
            ApplicationContextInitializerProvider.mContext=context;
            return true;
        }
        return false;
    }

    public static Context getContext(){
        return ApplicationContextInitializerProvider.mContext;
    }
    public static <A extends Application> A getApplication(){
        return (A) ApplicationContextInitializerProvider.mContext;
    }
}

