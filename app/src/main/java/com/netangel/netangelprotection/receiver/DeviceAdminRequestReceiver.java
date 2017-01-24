package com.netangel.netangelprotection.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.ui.LoginActivity;
import com.netangel.netangelprotection.ui.MainActivity;
import com.netangel.netangelprotection.util.Config;

public class DeviceAdminRequestReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        Config.saveBoolean(context, Config.IS_DEVICE_ADMIN, true);
        startMainActivity(context);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Config.saveBoolean(context, Config.IS_DEVICE_ADMIN, false);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.device_admin_description);
    }

    @VisibleForTesting
    protected void startMainActivity(Context context) {
        LoginActivity.start(context);
    }
}
