package com.netangel.netangelprotection.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;

import com.netangel.netangelprotection.model.CheckInResult;
import com.netangel.netangelprotection.restful.RestfulApi;
import com.netangel.netangelprotection.util.Config;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class CheckInService extends IntentService {

	private static final int REQUEST_CODE = 224;
	private static final long ONE_MIN = 60 * 1000;
	private static final long REGULAR_INTERVAL = 15 * ONE_MIN;

	public CheckInService() {
		super("CheckInService");
	}

	public static void start(Context context) {
		scheduleNextRun(context, ONE_MIN);
	}

	public static void stop(Context context) {
		scheduleNextRun(context, -1);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		RestfulApi api = RestfulApi.getInstance(this);
		Call<CheckInResult> call = api.checkIn();

		try {
			Response<CheckInResult> response = call.execute();
			if (response.isSuccessful()) {
				CheckInResult result = response.body();
				if (result != null && result.hasPendingChanges() && result.getChanges() != null) {

					boolean isBatterySaverOn = result.getChanges().isBatterySaver();
					Config.saveBoolean(this, Config.BATTERY_SAVER, isBatterySaverOn);

					boolean isEnableVpn = result.getChanges().isEnableVpn();
					Config.saveBoolean(this, Config.ENABLE_VPN, isEnableVpn);
					Config.saveBoolean(this, Config.IS_SWITCH_ON, isEnableVpn);

					boolean isPauseVpn = result.getChanges().isPauseVpn();
					Config.saveBoolean(this, Config.PAUSE_VPN, isPauseVpn);

					Intent i = new Intent("onVpnConfigurationChanged");
					LocalBroadcastManager.getInstance(this).sendBroadcast(i);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		scheduleNextRun(this, REGULAR_INTERVAL);
	}

	private static void scheduleNextRun(Context context, long timeout) {
		Intent intent = new Intent(context, CheckInService.class);
		PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pIntent);

		if (timeout < 0) {
			return;
		}

		long current = System.currentTimeMillis();
		long runTime = current + timeout;

		alarmManager.setExact(AlarmManager.RTC_WAKEUP, runTime, pIntent);
	}

}
