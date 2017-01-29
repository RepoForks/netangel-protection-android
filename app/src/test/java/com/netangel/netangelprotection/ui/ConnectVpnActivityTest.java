package com.netangel.netangelprotection.ui;

import android.content.Context;

import com.netangel.netangelprotection.NetAngelJunitSuite;
import com.netangel.netangelprotection.NetAngelRobolectricSuite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ConnectVpnActivityTest extends NetAngelJunitSuite {

    private ConnectVpnActivity activity;

    @Mock
    private Context context;

    @Before
    public void setUp() {
        activity = spy(new ConnectVpnActivity());
        doNothing().when(activity).restartActivity(context);
    }

    @Test
    public void shouldReconnect() {
        activity.setVpnConfirmed(false);
        activity.setForceConfirm(true);

        activity.getHomePressedReceiver().onReceive(context, null);

        verify(activity).restartActivity(context);
    }

    @Test
    public void hasAlreadyReconnected() {
        activity.setVpnConfirmed(true);
        activity.setForceConfirm(true);

        activity.getHomePressedReceiver().onReceive(context, null);

        verify(activity, times(0)).restartActivity(context);
    }

    @Test
    public void doesNotNeedToForceReconnect() {
        activity.setVpnConfirmed(false);
        activity.setForceConfirm(false);

        activity.getHomePressedReceiver().onReceive(context, null);

        verify(activity, times(0)).restartActivity(context);
    }
}