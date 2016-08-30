package com.netangel.netangelprotection.util;

import android.util.Log;

import com.netangel.netangelprotection.BuildConfig;

@SuppressWarnings("unused")
public class LogUtils {
    private LogUtils() {
    }

    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void d(String tag, String message, Throwable cause) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message, cause);
        }
    }

    public static void v(String tag, String message) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message);
        }
    }

    public static void v(String tag, String message, Throwable cause) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message, cause);
        }
    }

    public static void i(String tag, String message) {
        Log.i(tag, message);
    }

    public static void i(String tag, String message, Throwable cause) {
        Log.i(tag, message, cause);
    }

    public static void w(String tag, String message) {
        Log.w(tag, message);
    }

    public static void w(String tag, String message, Throwable cause) {
        Log.w(tag, message, cause);
    }

    public static void e(String tag, String message) {
        Log.e(tag, message);
    }

    public static void e(String tag, String message, Throwable cause) {
        Log.e(tag, message, cause);
    }
}
