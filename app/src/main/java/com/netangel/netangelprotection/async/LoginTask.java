package com.netangel.netangelprotection.async;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.netangel.netangelprotection.NetAngelApplication;
import com.netangel.netangelprotection.restful.RestfulApi;
import com.netangel.netangelprotection.ui.LoginActivity;
import com.netangel.netangelprotection.util.CommonUtils;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.LogUtils;
import com.netangel.netangelprotection.util.VpnHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import okhttp3.Credentials;
import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class LoginTask extends AsyncTask<Void, Void, Boolean> {
	private static final String TAG = LoginTask.class.getSimpleName();
	private static final String OVPN_FILE_NAME = "profile.ovpn";

	private final WeakReference<LoginActivity> loginActivityRef;
	private final String credential;

	/*
	 * Login with last known credential
	 */
	public LoginTask(@NonNull LoginActivity activity, String credential) {
		loginActivityRef = new WeakReference<>(activity);
		this.credential = credential;
	}

	public LoginTask(@NonNull LoginActivity activity, String email, String password) {
		loginActivityRef = new WeakReference<>(activity);
		this.credential = Credentials.basic(email, password);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		if (!login(credential)) {
			return false;
		}
		if (!Config.getBoolean(NetAngelApplication.getAppContext(), Config.IS_VPN_PROFILE_IMPORTED, false)) {
			if (!waitForVpnUser()) {
                return false;
            }
			if (!downloadOvpnFile()) {
                return false;
            }
			if (!importVpnProfile()) {
                return false;
            }
			Config.saveBoolean(NetAngelApplication.getAppContext(), Config.IS_VPN_PROFILE_IMPORTED, true);
		}
		return true;
	}

	private static boolean login(@NonNull String credential) {
		Call<ResponseBody> call = RestfulApi.getInstance().login(credential);
		try {
			Response<ResponseBody> response = call.execute();
			if (response.isSuccessful()) {
				saveAuthInfo(response.headers(), credential);
				return true;
			}
		} catch (Exception e) {
			LogUtils.w(TAG, "Failed to login", e);
		}
		return false;
	}

	private static void saveAuthInfo(@NonNull Headers headers, String credential) {
		Context c = NetAngelApplication.getAppContext();
		String sslCert = headers.get("X-Ssl-Cert");
		if (!TextUtils.isEmpty(sslCert)) {
			Config.saveString(c, Config.SSL_CERT, sslCert);
		}

		String secretToken = headers.get("X-Secret-Token");
		if (!TextUtils.isEmpty(secretToken)) {
			Config.saveString(c, Config.SECRET_TOKEN, secretToken);
		}

		String clientId = headers.get("X-Client-Id");
		if (!TextUtils.isEmpty(clientId)) {
			Config.saveString(c, Config.CLIENT_ID, clientId);
		}

//		Config.saveString(c, Config.CREDENTIAL, credential);
	}

	private static boolean waitForVpnUser() {
		while (true) {
			Call<Boolean> call = RestfulApi.getInstance().isVpnUserCreated();
			try {
				Response<Boolean> response = call.execute();
				if (response.isSuccessful()) {
					if (response.body()) {
						return true;
					}
					Thread.sleep(1000);
					continue;
				}
			} catch (Exception e) {
				LogUtils.w(TAG, "Failed to wait for VPN user", e);
			}
			return false;
		}
	}

	private static boolean downloadOvpnFile() {
		Call<ResponseBody> call = RestfulApi.getInstance().downloadOvpnFile();
		try {
			Response<ResponseBody> response = call.execute();
			if (response.isSuccessful()) {
				return saveOvpnFile(response.body());
			}
		} catch (Exception e) {
			LogUtils.w(TAG, "Failed to download ovpn-file", e);
		}
		return false;
	}

	private static boolean saveOvpnFile(@NonNull ResponseBody body) {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			File ovpnFile = new File(NetAngelApplication.getAppContext().getFilesDir(), OVPN_FILE_NAME);
			inputStream = body.byteStream();
			outputStream = new FileOutputStream(ovpnFile);
			byte[] buf = new byte[4096];
			while (true) {
				int read = inputStream.read(buf);
				if (read == -1) {
					break;
				}
				outputStream.write(buf, 0, read);
			}
			outputStream.flush();
			return true;
		} catch (Exception e) {
			LogUtils.w(TAG, "Failed to save ovpn-file", e);
			return false;
		} finally {
			CommonUtils.closeSilently(inputStream);
			CommonUtils.closeSilently(outputStream);
		}
	}

	private static boolean importVpnProfile() {
		File ovpnFile = new File(NetAngelApplication.getAppContext().getFilesDir(), OVPN_FILE_NAME);
		Uri uri = Uri.fromFile(ovpnFile);
		boolean isImported = VpnHelper.importProfileFromFile(uri);
		if (isImported) {
			//noinspection ResultOfMethodCallIgnored
			ovpnFile.delete();
		}
		return isImported;
	}

	@Override
	protected void onPostExecute(Boolean isSuccess) {
		LoginActivity activity = loginActivityRef.get();
		if (activity != null) {
			activity.onLoginFinished(isSuccess);
		}
	}
}
