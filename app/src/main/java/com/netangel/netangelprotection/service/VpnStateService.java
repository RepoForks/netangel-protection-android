package com.netangel.netangelprotection.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;

import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.ui.ConnectVpnActivity;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.ProtectionManager;

import de.blinkt.openvpn.core.VpnStatus;

public class VpnStateService extends Service implements VpnStatus.StateListener {

    @VisibleForTesting
    protected static boolean IS_RUNNING = false;

    @VisibleForTesting
    protected static int FOREGROUND_ID = 1001;

    public static void start(Context context) {
        if (!IS_RUNNING && Config.getBoolean(context, Config.IS_VPN_ENABLED, true)) {
            IS_RUNNING = true;
            context.startService(new Intent(context, VpnStateService.class));
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, VpnStateService.class));
    }

    private ProtectionManager protectionManager;

    @Override
    public void onCreate() {
        super.onCreate();

        this.protectionManager = ProtectionManager.getInstance();

        VpnStatus.addStateListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // by starting a background service as a foreground service, we are ensuring that
        // Android does not go in and wipe it out without us knowing.
        // https://developer.android.com/guide/components/services.html#Foreground
        startForeground(R.string.connecting_to_vpn, true);

        // We want this service to attempt to continue running until it is explicitly stopped
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        IS_RUNNING = false;

        // when we start it as sticky and foreground, the service should persist, but
        // this can be a sanity check to ensure it is kept running.
        start(this);
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId,
                            VpnStatus.ConnectionStatus level, VpnStatus.ConnectionStatus prevLevel) {

        if (level == VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT ||
                level == VpnStatus.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET ||
                level == VpnStatus.ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) {
            startForeground(R.string.connecting_to_vpn, true);
        } else if (level == VpnStatus.ConnectionStatus.LEVEL_CONNECTED) {
            protectionManager.setProtected(this, true);
            startForeground(R.string.device_protected, true);
        } else if (prevLevel == VpnStatus.ConnectionStatus.LEVEL_CONNECTED
                && level == VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED) {
            protectionManager.setProtected(this, false);
            startForeground(R.string.device_not_protected, true);

            if (protectionManager.isDisconnectedByApp()) {
                protectionManager.setDisconnectedByApp(false);
            } else if (Config.getBoolean(this, Config.IS_VPN_ENABLED, true)) {
                restartVpnConnection();
            }
        } else if (level == VpnStatus.ConnectionStatus.LEVEL_NONETWORK) {
            startForeground(R.string.failed_to_connect, true);
            protectionManager.setProtected(this, false);
        }
    }

    @VisibleForTesting
    protected void startForeground(@StringRes int text, boolean hideNotification) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.connection_status))
                .setContentText(getString(text))
                .setPriority(hideNotification ? NotificationCompat.PRIORITY_MIN : NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setWhen(0);

        startForeground(FOREGROUND_ID, builder.build());
    }

    @VisibleForTesting
    protected void setBaseContext(Context context) {
        attachBaseContext(context);
    }

    @VisibleForTesting
    protected void setProtectionManager(ProtectionManager manager) {
        this.protectionManager = manager;
    }

    @VisibleForTesting
    protected void restartVpnConnection() {
        ConnectVpnActivity.start(this, true);
    }
}
