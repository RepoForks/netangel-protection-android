package com.netangel.netangelprotection.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.HashMap;

/**
 * SharedPreferences Utilities
 * 
 */
public final class Config {

	public static final String CLIENT_ID = "client_id";
	public static final String SSL_CERT = "ssl_cert";
	public static final String SECRET_TOKEN = "secret_token";
//	public static final String CREDENTIAL = "credential";
	public static final String IS_VPN_PROFILE_IMPORTED = "is_vpn_profile_imported";
	public static final String IS_VPN_ENABLED = "is_switch_on";
	public static final String BATTERY_SAVER = "battery_saver";
	public static final String PAUSE_VPN = "pause_vpn";

	/**
	 * The name of SharedPreferences
	 */
	private static final String PREFERENCE_NAME = "com_netangel_netangelprotection";

	private Config() {}

	/**
	 * Get the instance of SharedPreferences
	 * 
	 * @param context
	 * @return
	 */
	public static SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
	}

	/**
	 * Save Single String preference
	 * 
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean saveString(Context context, String key, String value) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		return editor.commit();
	}

	/**
	 * Get string value through key
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static String getString(Context context, String key) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		return sharedPreferences.getString(key, "");
	}

	/**
	 * Save map String preference
	 * 
	 * @param context
	 * @param valuesMap
	 * @return
	 */
	public static boolean saveStringValues(Context context, HashMap<String, String> valuesMap) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		String value = "";
		for (String key : valuesMap.keySet()) {
			value = valuesMap.get(key);
			editor.putString(key, value);
		}
		return editor.commit();
	}

	/**
	 * Save single boolean preference
	 * 
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean saveBoolean(Context context, String key, boolean value) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putBoolean(key, value);
		return editor.commit();
	}

	/**
	 * Get boolean value through key
	 * 
	 * @param context
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBoolean(Context context, String key, boolean defaultValue) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		return sharedPreferences.getBoolean(key, defaultValue);
	}

	/**
	 * Clean the SharedPreferences
	 * 
	 * @param context
	 */
	public static void cleanSharedPreference(Context context) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.clear();
		editor.commit();
	}

	/**
	 * remove the value by key
	 * 
	 * @param context
	 * @param key
	 */
	public static void remove(Context context, String key) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.remove(key);
		editor.commit();
	}
	
	/**
	 * Save int value
	 * 
	 * @param context
	 * @param key
	 * @param value
	 * @return
	 */
	public static boolean saveInt(Context context, String key, int value) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putInt(key, value);
		return editor.commit();
	}

	/**
	 * Get int value
	 * 
	 * @param context
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public static int getInt(Context context, String key, int defaultValue) {
		SharedPreferences sharedPreferences = getSharedPreferences(context);
		return sharedPreferences.getInt(key, defaultValue);
	}

}
