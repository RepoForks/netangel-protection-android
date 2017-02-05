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
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.async.LoginTask;
import com.netangel.netangelprotection.util.CommonUtils;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.ProtectionManager;
import com.netangel.netangelprotection.util.VpnHelper;
import com.sromku.simple.storage.SimpleStorage;
import com.sromku.simple.storage.Storage;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.blinkt.openvpn.core.VpnStatus;

public class LoginActivity extends AppCompatActivity implements VpnStatus.StateListener {

	private final static int PERMISSIONS_REQUEST = 100;

	@BindView(R.id.edt_email)
	EditText email;

	@BindView(R.id.edt_password)
	EditText password;

	@BindView(R.id.txt_sign_up)
	TextView signup;

	private ProgressDialog progressDialog;
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

		signup.setPaintFlags(signup.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

		password.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					onClickSignIn();
					return true;
				}
				return false;
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
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
		if (level == VpnStatus.ConnectionStatus.LEVEL_START) {
			WaitingDialog.show(getSupportFragmentManager());
		} else if (prevLevel == VpnStatus.ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) {
			if (level == VpnStatus.ConnectionStatus.LEVEL_CONNECTED) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				ProtectionManager.getInstance().setProtected(this, true);
			} else if (level == VpnStatus.ConnectionStatus.LEVEL_AUTH_FAILED || level == VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED ) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				Snackbar.make(email, R.string.failed_to_connect, Snackbar.LENGTH_LONG).show();
			}
		}
	}

	@OnClick(R.id.btn_sign_in)
	public void onClickSignIn() {
		if (validateInput()) {
			if (CommonUtils.isInternetConnected(this)) {
				String email = this.email.getText().toString().trim();
				String password = this.password.getText().toString().trim();

				new LoginTask(this, email, password).execute();
				progressDialog = ProgressDialog.show(this, "", getString(R.string.connecting_to_vpn), true);
			} else {
				Snackbar.make(email, R.string.check_internet_connection, Snackbar.LENGTH_LONG).show();
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
		if (TextUtils.isEmpty(password.getText().toString())) {
			result = false;
			message = getResources().getString(R.string.enter_password_message);
		} else if (password.getText().toString().length() < 8) {
			result = false;
			message = getResources().getString(R.string.password_length_message);
		}

		// Email
		if (TextUtils.isEmpty(email.getText().toString())) {
			result = false;
			message = getResources().getString(R.string.enter_email_message);
		} else if (!CommonUtils.isValidEmail(email.getText().toString())) {
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

		if (isSuccess) {
			onLoginSuccessful();
		} else {
			onLoginFailed();
		}
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
