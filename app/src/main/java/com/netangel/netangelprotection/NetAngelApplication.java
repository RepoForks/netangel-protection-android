package com.netangel.netangelprotection;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.support.annotation.NonNull;

import com.netangel.netangelprotection.async.SetProtectedTask;
import com.netangel.netangelprotection.service.CheckInService;
import com.netangel.netangelprotection.service.VpnStateService;
import com.netangel.netangelprotection.ui.ConnectVpnActivity;
import com.netangel.netangelprotection.util.Config;

import de.blinkt.openvpn.core.PRNGFixes;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;

public class NetAngelApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PRNGFixes.apply();
        VpnStatus.initLogCache(getCacheDir());

        if (Config.getBoolean(this, Config.IS_SWITCH_ON, false)) {
            VpnStateService.start(this);
            CheckInService.start(this);
        }
    }
}
