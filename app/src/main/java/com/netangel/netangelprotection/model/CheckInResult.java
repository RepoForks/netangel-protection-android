package com.netangel.netangelprotection.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by DuyTran on 7/4/2016.
 */
public class CheckInResult {

	/*
	Sample results:
	{ "pending_changes": false }

	{
		"pending_changes": true,
		"changes": {
			"enable_vpn": true,
			"battery_saver": true,
			"pause_vpn": true
		}
	}
	*/

	@SerializedName("pending_changes")
	private boolean hasPendingChanges;

	@SerializedName("changes")
	private Change changes;

	public boolean hasPendingChanges() {
		return hasPendingChanges;
	}

	public void setHasPendingChanges(boolean hasPendingChanges) {
		this.hasPendingChanges = hasPendingChanges;
	}

	public Change getChanges() {
		return changes;
	}

	public void setChanges(Change changes) {
		this.changes = changes;
	}

	public class Change {

		@SerializedName("enable_vpn")
		private boolean enableVpn = true;

		@SerializedName("battery_saver")
		private boolean batterySaver = false;

		@SerializedName("pause_vpn")
		private boolean pauseVpn = false;

		public boolean isPauseVpn() {
			return pauseVpn;
		}

		public void setPauseVpn(boolean pauseVpn) {
			this.pauseVpn = pauseVpn;
		}

		public boolean isEnableVpn() {
			return enableVpn;
		}

		public void setEnableVpn(boolean enableVpn) {
			this.enableVpn = enableVpn;
		}

		public boolean isBatterySaver() {
			return batterySaver;
		}

		public void setBatterySaver(boolean batterySaver) {
			this.batterySaver = batterySaver;
		}
	}

}
