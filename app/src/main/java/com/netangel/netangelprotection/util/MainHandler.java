package com.netangel.netangelprotection.util;

import android.os.Handler;
import android.os.Looper;

public class MainHandler {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    private MainHandler() {
    }

    public static Handler get() {
        return sHandler;
    }
}
