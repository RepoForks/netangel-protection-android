package com.netangel.netangelprotection.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.blinkt.openvpn.core.VpnStatus;

public class LoginActivity extends AppCompatActivity implements VpnStatus.StateListener {

	private static final int PASSWORD_MIN_LENGTH = 8;

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
		if (validateInput(this, email.getText().toString(), password.getText().toString())) {
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

	@VisibleForTesting
	protected boolean validateInput(Context context, String email, String password) {
		String passwordMessage = validatePassword(context, password);
		String emailMessage = validateEmail(context, email);

		boolean inputOk = passwordMessage == null && emailMessage == null;
		if (!inputOk) {
			alertIncorrectPassword(emailMessage != null ? emailMessage : passwordMessage);
		}

		return inputOk;
	}

	@VisibleForTesting
	protected void alertIncorrectPassword(String message) {
		new AlertDialog.Builder(this)
				.setTitle(R.string.error)
				.setMessage(message)
				.setPositiveButton(R.string.ok, null)
				.show();
	}

	@VisibleForTesting
	protected String validatePassword(Context context, String password) {
		if (password == null || password.length() == 0) {
			return context.getString(R.string.enter_password_message);
		} else if (password.length() < PASSWORD_MIN_LENGTH) {
			return context.getString(R.string.password_length_message);
		}

		return null;
	}

	@VisibleForTesting
	protected String validateEmail(Context context, String email) {
		if (email == null || email.length() == 0) {
			return context.getString(R.string.enter_email_message);
		} else if (!isValidEmail(email)) {
			return context.getString(R.string.invalid_email_message);
		}

		return null;
	}

	@VisibleForTesting
	protected boolean isValidEmail(String email) {
		return CommonUtils.isValidEmail(email);
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
