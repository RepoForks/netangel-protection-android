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
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		boolean isBatterySaverOn = Config.getBoolean(this, Config.BATTERY_SAVER, false);

		RestfulApi api = RestfulApi.getInstance(this);
		Call<CheckInResult> call = api.checkIn();

		if (!isScreenOn(this) && isBatterySaverOn) {
			wifi.setWifiEnabled(true);
		}

		// Wait for Wifi to turn on
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			if (isNetworkAvailable(this))
				break;
		}

		try {
			Response<CheckInResult> response = call.execute();
			if (response.isSuccessful()) {
				CheckInResult result = response.body();
				if (result != null && result.hasPendingChanges() && result.getChanges() != null) {

					isBatterySaverOn = result.getChanges().isBatterySaver();
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

		if (!isScreenOn(this) && isBatterySaverOn) {
			wifi.setWifiEnabled(false);
		}

		scheduleNextRun(this, REGULAR_INTERVAL);
	}

	private boolean isNetworkAvailable(Context context) {
		ConnectivityManager connectivityManager
				= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public boolean isScreenOn(Context context) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
			DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
			boolean screenOn = false;
			for (Display display : dm.getDisplays()) {
				if (display.getState() != Display.STATE_OFF) {
					screenOn = true;
				}
			}
			return screenOn;
		} else {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			//noinspection deprecation
			return pm.isScreenOn();
		}
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
