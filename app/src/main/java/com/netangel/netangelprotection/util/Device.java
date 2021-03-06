package com.netangel.netangelprotection.util;

import android.content.Context;
import android.os.Build;

import com.netangel.netangelprotection.R;

import org.apache.commons.lang3.StringUtils;

public final class Device {

	private Device() {}

	public static String getName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer)) {
			return StringUtils.capitalize(model);
		} else {
			return StringUtils.capitalize(manufacturer + " " + model);
		}
	}

	public static String getDeviceType(Context context) {
		return context.getString(R.string.device_type);
	}

	public static String getModel() {
		return Build.MODEL;
	}

	public static String getOS() {
		return "Android";
	}

	public static String getOSVersion() {
		return Build.VERSION.RELEASE;
	}


}
