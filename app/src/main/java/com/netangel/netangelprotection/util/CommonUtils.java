package com.netangel.netangelprotection.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.netangel.netangelprotection.NetAngelApplication;

import java.io.Closeable;

public class CommonUtils {
    private CommonUtils() {
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable t) {
                // do nothing
            }
        }
    }

    public static boolean isInternetConnected() {
        ConnectivityManager cm = (ConnectivityManager) NetAngelApplication.getAppContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

}
