package com.netangel.netangelprotection.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.netangel.netangelprotection.service.CheckInService;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.VpnHelper;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ((Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
                && Config.getBoolean(context, Config.STATUS_PROTECTED, false)) {
			VpnProfile profile = VpnHelper.getProfile();
            if (profile != null) {
                launchVPN(context, profile);
                CheckInService.start();
            }
        }
    }

    private static void launchVPN(@NonNull Context context, @NonNull VpnProfile profile) {
        Intent startVpnIntent = new Intent(Intent.ACTION_MAIN);
        startVpnIntent.setClass(context, LaunchVPN.class);
        startVpnIntent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
        startVpnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startVpnIntent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);
        context.startActivity(startVpnIntent);
    }
}
