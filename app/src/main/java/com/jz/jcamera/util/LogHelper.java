package com.jz.jcamera.util;

import android.util.Log;

/**
 * usage:
 * create by jz on 20-2-24
 * email: 651410315@qq.com
 */
public class LogHelper {
    private static final String TAG = "j_tag";

    public static void de_i(String msg){
        Log.i(TAG, msg);
    }

    public static void de_e(String msg, Exception e) throws Exception {
        Log.i(TAG, msg);
        throw  e;
    }
}
