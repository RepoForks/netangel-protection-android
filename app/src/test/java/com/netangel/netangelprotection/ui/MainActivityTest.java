package com.netangel.netangelprotection.ui;

import android.content.Context;
import android.widget.TextView;

import com.netangel.netangelprotection.NetAngelJunitSuite;
import com.netangel.netangelprotection.NetAngelRobolectricSuite;
import com.netangel.netangelprotection.R;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class MainActivityTest extends NetAngelJunitSuite {

    private MainActivity activity;

    @Mock
    private TextView preventUninstall;
    @Mock
    private Context context;

    @Before
    public void setUp() {
        activity = spy(new MainActivity());
    }

    @Test
    public void shouldChangeUninstallButtonText() {
        doReturn(true).when(activity).isDeviceAdmin();
        activity.updateDeviceAdmin(preventUninstall);

        verify(preventUninstall).setText(R.string.allow_uninstall);
    }

    @Test
    public void shouldNotChangeUninstallButtonText() {
        doReturn(false).when(activity).isDeviceAdmin();
        activity.updateDeviceAdmin(preventUninstall);

        verify(preventUninstall).setText(R.string.prevent_uninstall);
    }
}