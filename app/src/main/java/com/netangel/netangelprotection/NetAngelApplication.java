package com.netangel.netangelprotection;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.support.annotation.NonNull;

import com.netangel.netangelprotection.async.SetProtectedTask;
import com.netangel.netangelprotection.ui.ConnectVpnActivity;
import com.netangel.netangelprotection.util.Config;

import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;

public class NetAngelApplication extends Application implements VpnStatus.StateListener {
    private static boolean isDisconnectedByApp;

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        PRNGFixes.apply();
        VpnStatus.initLogCache(getCacheDir());
        VpnStatus.addStateListener(this);

        if (Config.getBoolean(this, Config.STATUS_PROTECTED, false)) {
            ConnectVpnActivity.start(this, true);
        }
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId,
                            ConnectionStatus level, ConnectionStatus prevLevel) {
        if (prevLevel == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED
                && level == ConnectionStatus.LEVEL_CONNECTED) {
            setProtected(this, true);
        } else if (prevLevel == ConnectionStatus.LEVEL_CONNECTED
                && level == ConnectionStatus.LEVEL_NOTCONNECTED) {
            setProtected(this, false);
            if (isDisconnectedByApp) {
                isDisconnectedByApp = false;
            } else if (Config.getBoolean(this, Config.ENABLE_VPN, true)) {
                // Automatically reconnect to VPN if disconnected outside of the app.
                ConnectVpnActivity.start(this, true);
            }
        } else if (level == ConnectionStatus.LEVEL_NONETWORK) {
            setProtected(this, false);
        }
    }

    public static void setProtected(Context context, boolean isProtected) {
        if (isProtected != Config.getBoolean(context, Config.STATUS_PROTECTED, false)) {
            Config.saveBoolean(context, Config.STATUS_PROTECTED, isProtected);
            new SetProtectedTask(context).execute(isProtected);
        }
    }

    public static void setDisconnectedByApp(boolean isDisconnectedByApp) {
        NetAngelApplication.isDisconnectedByApp = isDisconnectedByApp;
    }
}
