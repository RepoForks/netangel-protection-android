package com.netangel.netangelprotection.async;

import android.os.AsyncTask;

import com.netangel.netangelprotection.restful.RestfulApi;
import com.netangel.netangelprotection.service.CheckInService;
import com.netangel.netangelprotection.util.CommonUtils;
import com.netangel.netangelprotection.util.LogUtils;
import com.netangel.netangelprotection.util.MainHandler;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class SetProtectedTask extends AsyncTask<Boolean, Void, Void> {
    private static final String TAG = SetProtectedTask.class.getSimpleName();
    private static final int RESEND_TIMEOUT = 5_000;

    private static final ResendRunnable resendRunnable = new ResendRunnable();

    @Override
    protected Void doInBackground(Boolean... params) {
        boolean isProtected = params[0];
        MainHandler.get().removeCallbacks(resendRunnable);
        if (!CommonUtils.isInternetConnected()) {
            LogUtils.v(TAG, "No Internet connection");
            resend(isProtected);
            return null;
        }

        boolean success = false;
        Call<ResponseBody> call;
        if (isProtected) {
            call = RestfulApi.getInstance().setProtected();
        } else {
            call = RestfulApi.getInstance().setUnprotected();
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
            CheckInService.start();
        } else {
            LogUtils.v(TAG, "Stop CheckInService");
            CheckInService.stop();
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
        private volatile boolean isProtected;

        void setProtected(boolean isProtected) {
            this.isProtected = isProtected;
        }

        @Override
        public void run() {
            new SetProtectedTask().execute(isProtected);
        }
    }
}
