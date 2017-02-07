package com.netangel.netangelprotection.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.netangel.netangelprotection.ui.ConnectVpnActivity;

public class VpnReconnectService extends IntentService {

    private static final String WAIT_TIME_BEFORE_REQUEST = "wait_time";

    public VpnReconnectService() {
        super("VpnReconnectService");
    }

    public static void start(Context context) {
        start(context, 0);
    }

    public static void start(Context context, long waitTime) {
        Intent connectService = new Intent(context, VpnReconnectService.class);
        connectService.putExtra(WAIT_TIME_BEFORE_REQUEST, waitTime);

        context.startService(connectService);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long waitTime = intent.getLongExtra(WAIT_TIME_BEFORE_REQUEST, 0L);

        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {

        }

        ConnectVpnActivity.start(this, true);
    }
}
