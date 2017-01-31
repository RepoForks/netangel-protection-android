package com.netangel.netangelprotection.async;

import android.content.Context;
import android.os.AsyncTask;

import com.netangel.netangelprotection.restful.RestfulApi;
import com.netangel.netangelprotection.service.CheckInService;
import com.netangel.netangelprotection.service.InternetAvailabilityCheckService;
import com.netangel.netangelprotection.util.CommonUtils;
import com.netangel.netangelprotection.util.LogUtils;
import com.netangel.netangelprotection.util.MainHandler;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class SetProtectedTask extends AsyncTask<Boolean, Void, Void> {
    private static final String TAG = SetProtectedTask.class.getSimpleName();
    private static final int RESEND_TIMEOUT = 5_000;

    private ResendRunnable resendRunnable;

    private Context context;
    private boolean isProtected;

    public SetProtectedTask(Context context, boolean isProtected) {
        this.context = context;
        this.resendRunnable = new ResendRunnable(context);
        this.isProtected = isProtected;
    }

    @Override
    protected Void doInBackground(Boolean... params) {
        MainHandler.get().removeCallbacks(resendRunnable);
        if (!CommonUtils.isInternetConnected(context)) {
            LogUtils.v(TAG, "No Internet connection");
            resend(isProtected);
            return null;
        }

        boolean success = false;
        Call<ResponseBody> call;
        if (isProtected) {
            call = RestfulApi.getInstance(context).setProtected();
        } else {
            call = RestfulApi.getInstance(context).setUnprotected();
        }
        try {
            Response<ResponseBody> response = call.execute();
            if (response.isSuccessful()) {
                success = true;
                LogUtils.v(TAG, isProtected ? "SetProtected success" : "SetUnprotected success");
            } else {
                LogUtils.v(TAG, isProtected ? "SetProtected error" : "SetUnprotected error");
            }
        } catch (Exception e) {
            LogUtils.w(TAG, isProtected ? "SetProtected failed" : "SetUnprotected failed", e);
        }

        if (!success) {
            resend(isProtected);
        } else if (isProtected) {
            LogUtils.v(TAG, "Start CheckInService");
            CheckInService.start(context);
            InternetAvailabilityCheckService.scheduleNextRun(context);
        } else {
            LogUtils.v(TAG, "Stop CheckInService");
            CheckInService.stop(context);
        }

        return null;
    }

    private void resend(boolean isProtected) {
        LogUtils.v(TAG, isProtected ? "Try to call SetProtected after " + RESEND_TIMEOUT + " ms"
                : "Try to call SetUnprotected after " + RESEND_TIMEOUT + " ms");
        resendRunnable.setProtected(isProtected);
        MainHandler.get().postDelayed(resendRunnable, RESEND_TIMEOUT);
    }

    private static class ResendRunnable implements Runnable {
        private Context context;
        private boolean isProtected;

        ResendRunnable(Context context) {
            this.context = context;
        }

        void setProtected(boolean isProtected) {
            this.isProtected = isProtected;
        }

        @Override
        public void run() {
            new SetProtectedTask(context, isProtected).execute();
        }
    }
}
