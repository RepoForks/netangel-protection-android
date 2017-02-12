package com.netangel.netangelprotection.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.async.LogoutTask;
import com.netangel.netangelprotection.service.CheckInService;
import com.netangel.netangelprotection.service.VpnStateService;
import com.netangel.netangelprotection.util.CommonUtils;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.ProtectionManager;
import com.netangel.netangelprotection.util.VpnHelper;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements VpnStatus.StateListener {

	@BindView(R.id.img_logo)
	GifImageView logo;

	@BindView(R.id.txt_device_protected_status)
	TextView protectionStatus;

	@BindView(R.id.txt_dashboard)
	TextView dashboardLink;

	@BindView(R.id.toggle_protect)
	ToggleButton protectToggle;

	private OpenVPNService service;
	private VpnHelper vpnHelper;
	private ProtectionManager protectionManager;
	private boolean ignoreUserLeaveHint;

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean switchOn = Config.getBoolean(MainActivity.this, Config.IS_VPN_ENABLED, true);
			boolean vpnConnected = new VpnHelper(context).isVpnConnected();

			if (switchOn && !vpnConnected) {
				connectVpn();
			} else {
				disconnectVpn();
			}

			updateUI();
		}
	};

	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			OpenVPNService.LocalBinder binder = (OpenVPNService.LocalBinder) service;
			MainActivity.this.service = binder.getService();

			boolean isEnableVpn = Config.getBoolean(MainActivity.this, Config.IS_VPN_ENABLED, true);
			if (!isEnableVpn) {
				disconnectVpn();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			MainActivity.this.service = null;
		}
	};

	public static void start(@NonNull Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		vpnHelper = new VpnHelper(this);
		protectionManager = ProtectionManager.getInstance();
		VpnStatus.addStateListener(this);

		IntentFilter intentFilter =
				new IntentFilter(CheckInService.VPN_CONFIGURATION_CHANGED_BROADCAST);
		registerReceiver(broadcastReceiver, intentFilter);

		dashboardLink.setPaintFlags(dashboardLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

		if (savedInstanceState == null) {
			boolean isSwitchOn = Config.getBoolean(this, Config.IS_VPN_ENABLED, false);
			if (isSwitchOn && !vpnHelper.isVpnConnected()) {
				connectVpn();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		unregisterReceiver(broadcastReceiver);
		VpnStatus.removeStateListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = new Intent(this, OpenVPNService.class);
		intent.setAction(OpenVPNService.START_SERVICE);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);

		updateUI();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindService(connection);
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		if (ignoreUserLeaveHint) {
			ignoreUserLeaveHint = false;
		} else {
			finish();
		}
	}

	@OnClick(R.id.btn_sign_out)
	public void onClickSignOut() {
		new LogoutTask(this).execute();
		LoginActivity.start(this);
		finish();
	}

	@OnClick(R.id.txt_dashboard)
	public void onClickDashboard() {
		String url = getString(R.string.URL_DASHBOARD);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(browserIntent);
	}

	private void updateUI() {
		boolean isProtected = vpnHelper.isVpnConnected();
		protectToggle.setChecked(isProtected);

		if (isProtected) {
			try {
				GifDrawable gifFromResource = new GifDrawable(getResources(), R.drawable.status_on_gif);
				gifFromResource.setLoopCount(1);
				logo.setImageDrawable(gifFromResource);
			} catch (IOException e) {
				logo.setImageResource(R.drawable.status_on);
				e.printStackTrace();
			}

			protectionStatus.setText(R.string.device_protected);
		} else {
			logo.setImageResource(R.drawable.status_off);
			protectionStatus.setText(R.string.device_not_protected);
		}
	}

	@OnClick(R.id.toggle_protect)
	public void onClickSwitchProtect() {
		if (protectToggle.isChecked()) {
			connectVpn();
		} else {
			disconnectVpn();
			Config.saveBoolean(this, Config.IS_VPN_ENABLED, false);
		}
	}

	private void connectVpn() {
		if (CommonUtils.isInternetConnected(this)) {
			ignoreUserLeaveHint = true;

			ConnectVpnActivity.start(this, false);
			VpnStateService.start(this);
		} else {
			Snackbar.make(protectToggle, R.string.check_internet_connection, Snackbar.LENGTH_LONG).show();
		}
	}

	private void disconnectVpn() {
		boolean isProtected = vpnHelper.isVpnConnected();

		if (isProtected) {
			ProfileManager.setConntectedVpnProfileDisconnected(this);
			if (service != null && service.getManagement() != null) {
                service.getManagement().stopVPN(false);
            }

			protectionManager.setDisconnectedByApp(true);
			protectionManager.setProtected(this, false);
			updateUI();
		}
	}

	@Override
	public void updateState(String state, String logmessage, int localizedResId,
							final ConnectionStatus level, final ConnectionStatus prevLevel) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateState(level, prevLevel);
			}
		});
	}

	private void updateState(final ConnectionStatus level, final ConnectionStatus prevLevel) {
		if (level == ConnectionStatus.LEVEL_START) {
			WaitingDialog.show(getSupportFragmentManager());
		} else if (prevLevel == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) {
			if (level == ConnectionStatus.LEVEL_CONNECTED) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				protectionManager.setProtected(this, true);
				Config.saveBoolean(this, Config.IS_VPN_ENABLED, true);
			} else if (level == ConnectionStatus.LEVEL_AUTH_FAILED || level == ConnectionStatus.LEVEL_NOTCONNECTED ) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				Snackbar.make(protectToggle, R.string.failed_to_connect, Snackbar.LENGTH_LONG).show();
			}
		}

		updateUI();
	}
}
