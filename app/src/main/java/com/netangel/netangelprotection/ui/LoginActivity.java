package com.netangel.netangelprotection.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.netangel.netangelprotection.NetAngelApplication;
import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.async.LoginTask;
import com.netangel.netangelprotection.util.CommonUtils;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.ProtectionManager;
import com.netangel.netangelprotection.util.VpnHelper;
import com.sromku.simple.storage.SimpleStorage;
import com.sromku.simple.storage.Storage;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.blinkt.openvpn.core.VpnStatus;

public class LoginActivity extends AppCompatActivity implements VpnStatus.StateListener {

	private final static int PERMISSIONS_REQUEST = 100;

	@BindView(R.id.edt_email)
	EditText edtEmail;

	@BindView(R.id.edt_password)
	EditText edtPassword;

	@BindView(R.id.txt_sign_up)
	TextView txtSignUp;

	private ProgressDialog progressDialog;
	private Storage storage;
	private VpnHelper helper;

	public static void start(@NonNull Context context) {
		Intent intent = new Intent(context, LoginActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		ButterKnife.bind(this);
		VpnStatus.addStateListener(this);
		helper = new VpnHelper(this);

		txtSignUp.setPaintFlags(txtSignUp.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

		edtPassword.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					onClickSignIn();
					return true;
				}
				return false;
			}
		});

		String clientId = Config.getString(this, Config.CLIENT_ID);
		if (TextUtils.isEmpty(clientId)) {
			if (checkStoragePermission()) {
				loadDataFromStorage();
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		connectToVpn(this, helper);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		VpnStatus.removeStateListener(this);
	}

	@Override
	public void updateState(String state, String logmessage, int localizedResId,
							final VpnStatus.ConnectionStatus level, final VpnStatus.ConnectionStatus prevLevel) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateState(level, prevLevel);
			}
		});
	}

	private void updateState(VpnStatus.ConnectionStatus level, VpnStatus.ConnectionStatus prevLevel) {
		if (level == VpnStatus.ConnectionStatus.LEVEL_START) {
			WaitingDialog.show(getSupportFragmentManager());
		} else if (prevLevel == VpnStatus.ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) {
			if (level == VpnStatus.ConnectionStatus.LEVEL_CONNECTED) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				ProtectionManager.getInstance().setProtected(this, true);
			} else if (level == VpnStatus.ConnectionStatus.LEVEL_AUTH_FAILED || level == VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED ) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				Snackbar.make(edtEmail, R.string.failed_to_connect, Snackbar.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0) {
					boolean result = true;

					for (int grantResult : grantResults) {
						if (grantResult != PackageManager.PERMISSION_GRANTED) {
							result = false;
						}
					}

					if (result)
						loadDataFromStorage();
				}
				return;
			}
		}
	}

	@OnClick(R.id.btn_sign_in)
	public void onClickSignIn() {
		if (validateInput()) {
			if (CommonUtils.isInternetConnected(this)) {
				String email = edtEmail.getText().toString().trim();
				String password = edtPassword.getText().toString().trim();
				new LoginTask(this, email, password).execute();
				progressDialog = ProgressDialog.show(this, "", getString(R.string.connecting_to_vpn), true);
			} else {
				Snackbar.make(edtEmail, R.string.check_internet_connection, Snackbar.LENGTH_LONG).show();
			}
		}
	}

	@OnClick(R.id.txt_sign_up)
	public void onClickSignUp() {
		String url = getResources().getString(R.string.URL_SIGN_UP);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(browserIntent);
	}

	private boolean validateInput() {
		boolean result = true;
		String message = "";

		// Password
		if (TextUtils.isEmpty(edtPassword.getText().toString())) {
			result = false;
			message = getResources().getString(R.string.enter_password_message);
		} else if (edtPassword.getText().toString().length() < 8) {
			result = false;
			message = getResources().getString(R.string.password_length_message);
		}

		// Email
		if (TextUtils.isEmpty(edtEmail.getText().toString())) {
			result = false;
			message = getResources().getString(R.string.enter_email_message);
		} else if (!CommonUtils.isValidEmail(edtEmail.getText().toString())) {
			result = false;
			message = getResources().getString(R.string.invalid_email_message);
		}

		if (!result) {
			new AlertDialog.Builder(this)
				.setTitle(R.string.error)
				.setMessage(message)
				.setPositiveButton(R.string.ok, null)
				.show();
		}

		return result;
	}

	public void onLoginFinished(boolean isSuccess) {
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}

		if (checkStoragePermission(false))
			saveDataToStorage();

		if (isSuccess) {
			onLoginSuccessful();
		} else {
			onLoginFailed();
		}
	}

	private boolean checkStoragePermission() {
		return checkStoragePermission(true);
	}

	private boolean checkStoragePermission(boolean shouldAskForPermission) {

		if (Build.VERSION.SDK_INT < 23)
			return true;

		ArrayList<String> permissions = new ArrayList<>();

		if (ContextCompat.checkSelfPermission(this,
			Manifest.permission.READ_EXTERNAL_STORAGE)
			!= PackageManager.PERMISSION_GRANTED) {
			permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
		}

		if (ContextCompat.checkSelfPermission(this,
			Manifest.permission.WRITE_EXTERNAL_STORAGE)
			!= PackageManager.PERMISSION_GRANTED) {
			permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
		}

		if (!permissions.isEmpty()) {
			if (shouldAskForPermission) {
				ActivityCompat.requestPermissions(this,
					permissions.toArray(new String[permissions.size()]),
					PERMISSIONS_REQUEST);
			}

			return false;
		}

		return true;
	}

	private void initStorage() {
		if (storage == null) {
			storage = SimpleStorage.getExternalStorage();

//			String IVX = "abcdefghijklmnop";
//			String SECRET_KEY = "secret1234567890";
//
//			SimpleStorageConfiguration configuration = new SimpleStorageConfiguration.Builder()
//				.setEncryptContent(IVX, SECRET_KEY)
//				.build();
//
//			SimpleStorage.updateConfiguration(configuration);
		}
	}

	private void loadDataFromStorage() {
		initStorage();

//		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
//			// only for gingerbread and newer versions
//		}

		if (storage.isFileExist(getResources().getString(R.string.app_name), Config.CLIENT_ID)
			&& storage.isFileExist(getResources().getString(R.string.app_name), Config.SSL_CERT)
			&& storage.isFileExist(getResources().getString(R.string.app_name), Config.SECRET_TOKEN)) {

			String clientId = storage.readTextFile(getResources().getString(R.string.app_name), Config.CLIENT_ID);
			Config.saveString(this, Config.CLIENT_ID, clientId);

			String sslCert = storage.readTextFile(getResources().getString(R.string.app_name), Config.SSL_CERT);
			Config.saveString(this, Config.SSL_CERT, sslCert);

			String secretToken = storage.readTextFile(getResources().getString(R.string.app_name), Config.SECRET_TOKEN);
			Config.saveString(this, Config.SECRET_TOKEN, secretToken);
		}
	}

	private void saveDataToStorage() {
		initStorage();

		storage.createDirectory(getResources().getString(R.string.app_name));

		String clientId = Config.getString(this, Config.CLIENT_ID);
		storage.createFile(getResources().getString(R.string.app_name), Config.CLIENT_ID, clientId);

		String secretToken = Config.getString(this, Config.SECRET_TOKEN);
		storage.createFile(getResources().getString(R.string.app_name), Config.SECRET_TOKEN, secretToken);

		String sslCert = Config.getString(this, Config.SSL_CERT);
		storage.createFile(getResources().getString(R.string.app_name), Config.SSL_CERT, sslCert);
	}

	private void onLoginSuccessful() {
		MainActivity.start(this);
		finish();
	}

	private void onLoginFailed() {
		new AlertDialog.Builder(this)
			.setTitle(R.string.error)
			.setMessage(getResources().getString(R.string.login_failed_message))
			.setPositiveButton(R.string.ok, null)
			.show();
	}

	@VisibleForTesting
	protected void connectToVpn(Context context, VpnHelper helper) {
		if (isSwitchOn(context) && !helper.isVpnConnected()) {
			startVpnConnectionActivity(context);
		}
	}

	@VisibleForTesting
	protected boolean isSwitchOn(Context context) {
		return Config.getBoolean(context, Config.IS_SWITCH_ON, false);
	}

	@VisibleForTesting
	protected void startVpnConnectionActivity(Context context) {
		ConnectVpnActivity.start(context, false);
	}
}
