package com.sad.jetpack.v1.appgo.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 初始化行为合规性检测和配置
 */
public class ComplianceChecker {

    public static boolean accessAble(Context context,String k){
        try {
            SharedPreferences sp=context.getSharedPreferences("APPGO_COMPLIANCE_CHECKER",Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
            sp.getBoolean(k,false);
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public static void setAccessAble(Context context,String k,boolean enable){
        config(context,k,enable);
    }

    private static void config(Context context,String k,boolean a){
        try {
            SharedPreferences sp=context.getSharedPreferences("APPGO_COMPLIANCE_CHECKER",Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
            sp.edit().putBoolean(k,a).commit();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
