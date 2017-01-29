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
import android.support.annotation.VisibleForTesting;
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
		RestfulApi api = getApi();
		Call<CheckInResult> call = api.checkIn();

		try {
			ResponseWrapper wrapper = execute(call);
			if (wrapper.isSuccessful) {
				CheckInResult result = wrapper.body;
				if (result != null && result.hasPendingChanges() && result.getChanges() != null) {

					boolean isBatterySaverOn = result.getChanges().isBatterySaver();
					saveBoolean(Config.BATTERY_SAVER, isBatterySaverOn);

					boolean isEnableVpn = result.getChanges().isEnableVpn();
					saveBoolean(Config.ENABLE_VPN, isEnableVpn);
					saveBoolean(Config.IS_SWITCH_ON, isEnableVpn);

					boolean isPauseVpn = result.getChanges().isPauseVpn();
					saveBoolean(Config.PAUSE_VPN, isPauseVpn);

					sendBroadcast("onVpnConfigurationChanged");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		scheduleNextRun(this, REGULAR_INTERVAL);
	}

	@VisibleForTesting
	protected ResponseWrapper execute(Call<CheckInResult> call) throws IOException {
		return new ResponseWrapper(call.execute());
	}

	@VisibleForTesting
	protected RestfulApi getApi() {
		return RestfulApi.getInstance(this);
	}

	@VisibleForTesting
	protected void saveBoolean(String id, boolean value) {
		Config.saveBoolean(this, id, value);
	}

	@VisibleForTesting
	protected void sendBroadcast(String value) {
		Intent i = new Intent(value);
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
	}

	@VisibleForTesting
	protected static void scheduleNextRun(Context context, long timeout) {
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

	protected static class ResponseWrapper {
		boolean isSuccessful;
		CheckInResult body;

		ResponseWrapper(Response<CheckInResult> response) {
			this.isSuccessful = response.isSuccessful();
			this.body = response.body();
		}
	}

}
