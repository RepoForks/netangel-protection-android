package com.netangel.netangelprotection.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.netangel.netangelprotection.service.VpnStateService;
import com.netangel.netangelprotection.ui.ConnectVpnActivity;
import com.netangel.netangelprotection.util.Config;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ((Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
                && isSwitchOn(context)) {
            startVpnConnection(context);
        }
    }

    @VisibleForTesting
    protected boolean isSwitchOn(Context context) {
        return Config.getBoolean(context, Config.IS_VPN_ENABLED, false);
    }

    @VisibleForTesting
    protected void startVpnConnection(Context context) {
        VpnStateService.start(context);
        ConnectVpnActivity.start(context, true);
    }
}
