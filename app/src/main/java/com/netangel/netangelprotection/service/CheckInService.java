package com.netangel.netangelprotection.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.netangel.netangelprotection.NetAngelApplication;
import com.netangel.netangelprotection.async.CheckInTask;

public class CheckInService extends Service {

	public static final String ON = "on";
	public static final String OFF = "off";

	private static final long INTERVAL = 15 * 60 * 1000;

	private Handler handler = null;
	private Runnable runnable = null;

	{
		runnable = new Runnable() {
			public void run() {
				new CheckInTask(CheckInService.this).execute();
				handler.postDelayed(runnable, INTERVAL);
			}
		};
	}

	public CheckInService() {
	}

	public static void start(Context context) {
		Intent i = new Intent(context, CheckInService.class);
		context.startService(i);
	}

	public static void stop(Context context) {
		Intent i = new Intent(context, CheckInService.class);
		i.setAction(CheckInService.OFF);
		context.startService(i);
	}

	@Override
	public void onDestroy() {
		if (handler != null) {
			handler.removeCallbacks(runnable);
			handler = null;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String mode = intent.getAction();
			if (OFF.equals(mode)) {
				stopSelf();
				return START_NOT_STICKY;
			}
		}
		if (handler == null)
			handler = new Handler();

		handler.removeCallbacks(runnable);
		handler.post(runnable);
		return START_STICKY;
	}

}
