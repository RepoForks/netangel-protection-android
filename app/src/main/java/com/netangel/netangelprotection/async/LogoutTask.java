package com.netangel.netangelprotection.async;

import android.content.Context;
import android.os.AsyncTask;

import com.netangel.netangelprotection.service.CheckInService;
import com.netangel.netangelprotection.service.VpnStateService;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.VpnHelper;

public class LogoutTask extends AsyncTask<Void, Void, Void> {
	private Context context;

	public LogoutTask(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... params) {
		Config.remove(context, Config.IS_VPN_ENABLED);
		Config.remove(context, Config.IS_VPN_PROFILE_IMPORTED);

		Config.remove(context, Config.CLIENT_ID);
		Config.remove(context, Config.SECRET_TOKEN);
		Config.remove(context, Config.SSL_CERT);

		new VpnHelper(context).removeProfiles();

		CheckInService.stop(context);
		VpnStateService.stop(context);

		return null;
	}
}
