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

import com.netangel.netangelprotection.NetAngelApplication;
import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.async.LogoutTask;
import com.netangel.netangelprotection.service.VpnStateService;
import com.netangel.netangelprotection.util.CommonUtils;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.ProtectionManager;

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
	GifImageView imgLogo;

	@BindView(R.id.txt_device_protected_status)
	TextView txtDeviceProtectedStatus;

	@BindView(R.id.txt_dashboard)
	TextView txtDashboard;

	@BindView(R.id.toggle_protect)
	ToggleButton switchProtect;

	private OpenVPNService service;
	private ProtectionManager protectionManager;
	private boolean ignoreUserLeaveHint;

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			boolean isEnableVpn = Config.getBoolean(MainActivity.this, Config.ENABLE_VPN, true);
			if (isEnableVpn) {
				boolean isPauseVpn = Config.getBoolean(MainActivity.this, Config.PAUSE_VPN, false);
				if (isPauseVpn) {
					pauseVpn(OpenVPNManagement.pauseReason.userPause);
				} else {
					resumeVpn();
				}
			} else {
				disconnectVpn();
				Config.remove(MainActivity.this, Config.ENABLE_VPN);
			}

			boolean isBatterySaverOn = Config.getBoolean(MainActivity.this, Config.BATTERY_SAVER, true);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean("screenoff", isBatterySaverOn);
			editor.commit();
		}
	};

	private final ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			OpenVPNService.LocalBinder binder = (OpenVPNService.LocalBinder) service;
			MainActivity.this.service = binder.getService();

			boolean isEnableVpn = Config.getBoolean(MainActivity.this, Config.ENABLE_VPN, true);
			if (!isEnableVpn) {
				disconnectVpn();
				Config.remove(MainActivity.this, Config.ENABLE_VPN);
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

		protectionManager = ProtectionManager.getInstance();
		VpnStatus.addStateListener(this);

		IntentFilter intentFilter = new IntentFilter("onVpnConfigurationChanged");
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

		txtDashboard.setPaintFlags(txtDashboard.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

		if (savedInstanceState == null) {
			boolean isSwitchOn = Config.getBoolean(this, Config.IS_SWITCH_ON, false);
			boolean isProtected = Config.getBoolean(this, Config.STATUS_PROTECTED, false);
			if (isSwitchOn && !isProtected) {
				connectVpn();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
		VpnStatus.removeStateListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Intent intent = new Intent(this, OpenVPNService.class);
		intent.setAction(OpenVPNService.START_SERVICE);
		bindService(intent, connection, Context.BIND_AUTO_CREATE);

//		resumeVpn();
		updateUI();
	}

	@Override
	protected void onPause() {
		super.onPause();
//		boolean isBatterySaverOn = Config.getBoolean(this, Config.BATTERY_SAVER, false);
//		if (isBatterySaverOn) {
//			pauseVpn(OpenVPNManagement.pauseReason.screenOff);
//		}
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

	private void updateUI() {
		boolean isProtected = Config.getBoolean(this, Config.STATUS_PROTECTED, false);
		switchProtect.setChecked(isProtected);
		if (isProtected) {
			try {
				GifDrawable gifFromResource = new GifDrawable(getResources(), R.drawable.status_on_gif);
				gifFromResource.setLoopCount(1);
				imgLogo.setImageDrawable(gifFromResource);
			} catch (IOException e) {
				imgLogo.setImageResource(R.drawable.status_on);
				e.printStackTrace();
			}
			txtDeviceProtectedStatus.setText(R.string.device_protected);
		} else {
			imgLogo.setImageResource(R.drawable.status_off);
			txtDeviceProtectedStatus.setText(R.string.device_not_protected);
		}
	}

	@OnClick(R.id.toggle_protect)
	public void onClickSwitchProtect() {
		if (switchProtect.isChecked()) {
			connectVpn();
		} else {
			disconnectVpn();
			Config.saveBoolean(this, Config.IS_SWITCH_ON, false);
		}
	}

	private void connectVpn() {
		if (CommonUtils.isInternetConnected(this)) {
			ignoreUserLeaveHint = true;

			ConnectVpnActivity.start(this, false);
			VpnStateService.start(this);
		} else {
			Snackbar.make(switchProtect, R.string.check_internet_connection, Snackbar.LENGTH_LONG).show();
			updateUI();

			VpnStateService.stop(this);
		}
	}

	private void disconnectVpn() {
		boolean isProtected = Config.getBoolean(this, Config.STATUS_PROTECTED, false);
		if (isProtected) {
			ProfileManager.setConntectedVpnProfileDisconnected(this);
			if (service != null && service.getManagement() != null) {
                service.getManagement().stopVPN(false);
            }

			protectionManager.setDisconnectedByApp(true);
			protectionManager.setProtected(this, false);
			updateUI();

			VpnStateService.stop(this);
		}
	}

	private void pauseVpn(OpenVPNManagement.pauseReason reason) {
		boolean isProtected = Config.getBoolean(this, Config.STATUS_PROTECTED, false);
		if (isProtected) {
			Intent intent = new Intent(this, OpenVPNService.class);
			intent.setAction(OpenVPNService.PAUSE_VPN);
			startService(intent);

			if (service != null && service.getManagement() != null) {
				service.getManagement().pause(reason);
			}

			VpnStateService.stop(this);
		}
	}

	private void resumeVpn() {
		boolean isProtected = Config.getBoolean(this, Config.STATUS_PROTECTED, false);
		if (isProtected) {
			Intent intent = new Intent(this, OpenVPNService.class);
			intent.setAction(OpenVPNService.RESUME_VPN);
			startService(intent);

			if (service != null && service.getManagement() != null) {
				service.getManagement().resume();
			}

			VpnStateService.start(this);
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

	private void updateState(ConnectionStatus level, ConnectionStatus prevLevel) {
		if (level == ConnectionStatus.LEVEL_START) {
			WaitingDialog.show(getSupportFragmentManager());
		} else if (prevLevel == ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED) {
			if (level == ConnectionStatus.LEVEL_CONNECTED) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				protectionManager.setProtected(this, true);
				Config.saveBoolean(this, Config.IS_SWITCH_ON, true);
			} else if (level == ConnectionStatus.LEVEL_AUTH_FAILED || level == ConnectionStatus.LEVEL_NOTCONNECTED ) {
				WaitingDialog.dismiss(getSupportFragmentManager());
				Snackbar.make(switchProtect, R.string.failed_to_connect, Snackbar.LENGTH_LONG).show();
			}
		}
		updateUI();
	}

	@OnClick(R.id.btn_sign_out)
	public void onClickSignOut() {
		new LogoutTask().execute();
		LoginActivity.start(this);
		finish();
	}

	@OnClick(R.id.txt_dashboard)
	public void onClickDashboard() {
		String url = getString(R.string.URL_DASHBOARD);
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(browserIntent);
	}
}
