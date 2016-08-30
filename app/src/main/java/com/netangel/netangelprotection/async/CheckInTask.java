package com.netangel.netangelprotection.async;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;

import com.netangel.netangelprotection.NetAngelApplication;
import com.netangel.netangelprotection.model.CheckInResult;
import com.netangel.netangelprotection.restful.RestfulApi;
import com.netangel.netangelprotection.util.Config;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by DuyTran on 6/21/2016.
 */
public class CheckInTask extends AsyncTask<Void, Void, Void> {

	public CheckInTask() {}

	@Override
	protected Void doInBackground(Void... params) {
		Context c = NetAngelApplication.getAppContext();
		WifiManager wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
		boolean isBatterySaverOn = Config.getBoolean(c, Config.BATTERY_SAVER, false);

		RestfulApi api = RestfulApi.getInstance();
		Call<CheckInResult> call = api.checkIn();

		if (!isScreenOn(c) && isBatterySaverOn) {
			wifi.setWifiEnabled(true);
		}

		// Wait for Wifi to turn on
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			if (isNetworkAvailable(c))
				break;
		}

		try {

			Response<CheckInResult> response = call.execute();
			if (response.isSuccessful()) {
				CheckInResult result = response.body();
				if (result != null && result.hasPendingChanges() && result.getChanges() != null) {

                    isBatterySaverOn = result.getChanges().isBatterySaver();
                    Config.saveBoolean(c, Config.BATTERY_SAVER, isBatterySaverOn);

                    boolean isEnableVpn = result.getChanges().isEnableVpn();
                    Config.saveBoolean(c, Config.ENABLE_VPN, isEnableVpn);
                    Config.saveBoolean(c, Config.IS_SWITCH_ON, isEnableVpn);

                    boolean isPauseVpn = result.getChanges().isPauseVpn();
                    Config.saveBoolean(c, Config.PAUSE_VPN, isPauseVpn);

                    Intent i = new Intent("onVpnConfigurationChanged");
                    LocalBroadcastManager.getInstance(c).sendBroadcast(i);
                }
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!isScreenOn(c) && isBatterySaverOn) {
			wifi.setWifiEnabled(false);
		}

		return null;
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

}
