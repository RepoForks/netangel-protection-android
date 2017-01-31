package com.netangel.netangelprotection.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.netangel.netangelprotection.ui.ConnectVpnActivity;
import com.netangel.netangelprotection.util.Config;

import java.net.HttpURLConnection;
import java.net.URL;

public class InternetAvailabilityCheckService extends IntentService {

    private static final int REQUEST_CODE = 225;
    private static final long ONE_MIN = 60 * 1000;
    private static final long REGULAR_INTERVAL = 10 * ONE_MIN;
    private static final String GOOGLE_GENERATE_204 = "http://clients3.google.com/generate_204";

    public InternetAvailabilityCheckService() {
        super("InternetAvailabilityCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (isSwitchOn() && !hasConnection()) {
            restartVpnConnection();
        }

        scheduleNextRun(this);
    }

    @VisibleForTesting
    protected boolean isSwitchOn() {
        return Config.getBoolean(this, Config.IS_SWITCH_ON, false);
    }

    @VisibleForTesting
    protected boolean hasConnection() {
        try {
            HttpURLConnection urlConnection =
                    (HttpURLConnection) new URL(GOOGLE_GENERATE_204).openConnection();
            urlConnection.setConnectTimeout(2000);
            urlConnection.connect();

            return (urlConnection.getResponseCode() == 204 &&
                    urlConnection.getContentLength() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    @VisibleForTesting
    protected void restartVpnConnection() {
        ConnectVpnActivity.start(this, true);
    }

    public static void scheduleNextRun(Context context) {
        Intent intent = new Intent(context, InternetAvailabilityCheckService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pIntent);

        long current = System.currentTimeMillis();
        long runTime = current + REGULAR_INTERVAL;

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, runTime, pIntent);
    }
}
