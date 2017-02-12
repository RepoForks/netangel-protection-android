package com.netangel.netangelprotection.util;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.netangel.netangelprotection.async.SetProtectedTask;

/**
 * Manages the state of the protection status and saves it to the configuration.
 */
public class ProtectionManager {

    private static volatile ProtectionManager manager;
    public static ProtectionManager getInstance() {
        if (manager == null) {
            manager = new ProtectionManager();
        }

        return manager;
    }

    private boolean isDisconnectedByApp;

    private ProtectionManager() {
        this.isDisconnectedByApp = false;
    }

    public void setProtected(Context context, boolean isProtected) {
        if (isProtected != isCurrentlyProtected(context)) {
            startTask(context, isProtected);
        }
    }

    public void setDisconnectedByApp(boolean isDisconnectedByApp) {
         this.isDisconnectedByApp = isDisconnectedByApp;
    }

    public boolean isDisconnectedByApp() {
        return isDisconnectedByApp;
    }

    @VisibleForTesting
    protected boolean isCurrentlyProtected(Context context) {
        return new VpnHelper(context).isVpnConnected();
    }
    @VisibleForTesting
    protected void startTask(Context context, boolean isProtected) {
        new SetProtectedTask(context, isProtected).execute();
    }
}
