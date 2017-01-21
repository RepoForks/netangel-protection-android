package com.netangel.netangelprotection.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.netangel.netangelprotection.BuildConfig;
import com.netangel.netangelprotection.NetAngelApplication;
import com.netangel.netangelprotection.util.VpnHelper;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.VPNLaunchHelper;
import de.blinkt.openvpn.core.VpnStatus;

public class ConnectVpnActivity extends AppCompatActivity {
    private static final String EXTRA_FORCE_CONFIRM = BuildConfig.APPLICATION_ID + ".ForceConfirm";
    private static final int REQUEST_CONFIRM_VPN = 0;

    private boolean forceConfirm;

    public static void start(Context context, boolean forceConfirm) {
        Intent intent = new Intent(context, ConnectVpnActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_FORCE_CONFIRM, forceConfirm);
        context.startActivity(intent);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        forceConfirm = getIntent().getBooleanExtra(EXTRA_FORCE_CONFIRM, false);
        if (savedInstanceState == null) {
            launchVpn();
        }
    }

    private void launchVpn() {
        Intent intent = new VpnHelper(this).prepareVpnService();
        if (intent != null) {
            VpnStatus.updateStateString("USER_VPN_PERMISSION", "",
                    de.blinkt.openvpn.R.string.state_user_vpn_permission,
                    VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT);
            try {
                startActivityForResult(intent, REQUEST_CONFIRM_VPN);
            } catch (ActivityNotFoundException e) {
                VpnStatus.logError(de.blinkt.openvpn.R.string.no_vpn_support_image);
                finish();
            }
        } else {
            onVpnConfirmed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONFIRM_VPN) {
            if (resultCode == RESULT_OK) {
                onVpnConfirmed();
            } else {
                onVpnCanceled();
            }
        }
    }

    private void onVpnConfirmed() {
        VpnProfile profile = new VpnHelper(this).getProfile();
        if (profile != null) {
            VPNLaunchHelper.startOpenVpn(profile, this);
        }
        finish();
    }

    private void onVpnCanceled() {
        VpnStatus.updateStateString("USER_VPN_PERMISSION_CANCELLED", "",
                de.blinkt.openvpn.R.string.state_user_vpn_permission_cancelled,
                VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED);
        if (forceConfirm) {
            launchVpn();
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(0, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}
