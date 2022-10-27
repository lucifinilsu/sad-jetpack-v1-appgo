package com.sad.jetpack.v1.appgo.api;

import android.app.ActivityManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class ApplicationLifecycleObserverMaster {

    /*public static String getCurrAppProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> mActivityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess :) {
            if (appProcess.pid == pid) {

                return appProcess.processName;
            }
        }
        return "";
    }*/
    private static String cacheCurrAppProccessName="";
    public static boolean isLog=true;
    private static void logCaller(){
        log(Log.getStackTraceString(new Throwable()));
    }
    private static void log(String s){
        if (isLog){
            Log.e("sad-basic",s);
        }
    }
    /**
     * 获取当前运行的进程名
     * @param context
     * @return
     */
    public static String getCurrAppProcessName(Context context){
        logCaller();
        return getCurrAppProcessName(context,true);
    }

    public static String getCurrAppProcessName(Context context,boolean readCache) {
        if (readCache){
            if (!TextUtils.isEmpty(cacheCurrAppProccessName)){
                log("-------->获取进程名缓存:"+cacheCurrAppProccessName);
                return cacheCurrAppProccessName;
            }
        }
        try {
            cacheCurrAppProccessName=getCurrAppProccessName2();
            log("-------->获取进程名v2:"+cacheCurrAppProccessName);
            if (!TextUtils.isEmpty(cacheCurrAppProccessName)){
                return cacheCurrAppProccessName;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(cacheCurrAppProccessName)){
            int pid = android.os.Process.myPid();
            ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            int i=0;
            List<ActivityManager.RunningAppProcessInfo> list=mActivityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo appProcess : list) {
                i++;
                if (appProcess.pid == pid) {
                    log("获取进程名v1循环:"+i);
                    cacheCurrAppProccessName= appProcess.processName;
                    return cacheCurrAppProccessName;
                }
            }
        }
        return "";
    }
    private static String getCurrAppProccessName2(){
        FileInputStream var1 = null;
        String var7;
        try {
            String var2 = "/proc/self/cmdline";
            var1 = new FileInputStream(var2);
            byte[] var3 = new byte[256];
            int var4;
            int var5;
            for(var4 = 0; (var5 = var1.read()) > 0 && var4 < var3.length; var3[var4++] = (byte)var5) {
            }
            if (var4 <= 0) {
                return null;
            }
            String var6 = new String(var3, 0, var4, "UTF-8");
            var7 = var6;
        } catch (Throwable var18) {
            var18.printStackTrace();
            return null;
        } finally {
            if (var1 != null) {
                try {
                    var1.close();
                } catch (IOException var17) {
                    var17.printStackTrace();
                }
            }

        }
        return var7;
    }

    /*public static void doOnCreatedAnchor(IApplicationLifecyclesObserver lifecyclesObserver, String[] processNames,boolean after,Application application){
        String currProcess=getCurrAppProccessName(application);
        if (processNames==null || processNames.length==0 || java.util.Arrays.asList(processNames).contains(currProcess)){
            if (after){
                lifecyclesObserver.onApplicationCreated(application);
            }
            else {
                lifecyclesObserver.onApplicationPreCreated(application);
            }

        }
    }

    public static void doOnConfigurationChangedAnchor(IApplicationLifecyclesObserver lifecyclesObserver, String[] processNames, boolean after, Application application, Configuration newConfig){
        String currProcess=getCurrAppProccessName(application);
        if (processNames==null || processNames.length==0 || java.util.Arrays.asList(processNames).contains(currProcess)){
            if (after){
                lifecyclesObserver.onApplicationConfigurationChanged(application,newConfig);
            }
            else {
                lifecyclesObserver.onApplicationPreConfigurationChanged(application,newConfig);
            }

        }
    }

    public static void doOnLowMemoryAnchor(IApplicationLifecyclesObserver lifecyclesObserver, String[] processNames,boolean after,Application application){
        String currProcess=getCurrAppProccessName(application);
        if (processNames==null || processNames.length==0 || java.util.Arrays.asList(processNames).contains(currProcess)){
            if (after){
                lifecyclesObserver.onApplicationLowMemory(application);
            }
            else {
                lifecyclesObserver.onApplicationPreLowMemory(application);
            }

        }
    }

    public static void doOnTerminateAnchor(IApplicationLifecyclesObserver lifecyclesObserver, String[] processNames,boolean after,Application application){
        String currProcess=getCurrAppProccessName(application);
        if (processNames==null || processNames.length==0 || java.util.Arrays.asList(processNames).contains(currProcess)){
            if (after){
                lifecyclesObserver.onApplicationTerminate(application);
            }
            else {
                lifecyclesObserver.onApplicationPreTerminate(application);
            }

        }
    }
    public static void doOnTrimMemoryAnchor(IApplicationLifecyclesObserver lifecyclesObserver, String[] processNames,boolean after,Application application,int level){
        String currProcess=getCurrAppProccessName(application);
        if (processNames==null || processNames.length==0 || java.util.Arrays.asList(processNames).contains(currProcess)){
            if (after){
                lifecyclesObserver.onApplicationTrimMemory(application,level);
            }
            else {
                lifecyclesObserver.onApplicationPreTrimMemory(application,level);
            }

        }
    }
    public static void doAttachApplicationBaseContextAnchor(IApplicationLifecyclesObserver lifecyclesObserver, String[] processNames,boolean after,Context context){
        String currProcess=getCurrAppProccessName(context);
        if (processNames==null || processNames.length==0 || java.util.Arrays.asList(processNames).contains(currProcess)){
            if (after){
                lifecyclesObserver.attachApplicationBaseContext(context);
            }
            else {
                lifecyclesObserver.attachApplicationPreBaseContext(context);
            }

        }
    }*/
}
