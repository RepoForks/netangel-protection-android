package com.netangel.netangelprotection.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatDialogFragment;

import com.netangel.netangelprotection.R;

public class WaitingDialog extends AppCompatDialogFragment {
    private static final String TAG = WaitingDialog.class.getSimpleName();

    public static void show(@NonNull FragmentManager fm) {
        WaitingDialog dialog = (WaitingDialog) fm.findFragmentByTag(TAG);
        if (dialog == null) {
            dialog = new WaitingDialog();
            dialog.setCancelable(false);
            fm.beginTransaction().add(dialog, TAG).commitAllowingStateLoss();
        }
    }

    public static void dismiss(@NonNull FragmentManager fm) {
        WaitingDialog dialog = (WaitingDialog) fm.findFragmentByTag(TAG);
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage(getString(R.string.please_wait));
        return dialog;
    }
}
