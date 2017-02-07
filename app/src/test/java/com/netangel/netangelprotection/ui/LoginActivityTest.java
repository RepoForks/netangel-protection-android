package com.netangel.netangelprotection.ui;

import android.app.Activity;
import android.content.Context;

import com.netangel.netangelprotection.NetAngelJunitSuite;
import com.netangel.netangelprotection.util.VpnHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class LoginActivityTest extends NetAngelJunitSuite {

    private LoginActivity loginActivity;

    @Mock
    private VpnHelper vpnHelper;
    @Mock
    private Context context;

    @Before
    public void setUp() {
        loginActivity = spy(new LoginActivity());
        doNothing().when(loginActivity).startVpnConnectionActivity(context);
    }

    @Test
    public void shouldConnectToVpnWhenHelperSaysItIsNotConnected() {
        doReturn(true).when(loginActivity).isSwitchOn(context);
        doReturn(false).when(vpnHelper).isVpnConnected();

        loginActivity.connectToVpn(context, vpnHelper);

        verify(loginActivity).startVpnConnectionActivity(context);
    }

    @Test
    public void shouldNotConnectToVpnIfHelperSaysItIsOn() {
        doReturn(true).when(loginActivity).isSwitchOn(context);
        doReturn(true).when(vpnHelper).isVpnConnected();

        loginActivity.connectToVpn(context, vpnHelper);

        verify(loginActivity, times(0)).startVpnConnectionActivity(context);
    }

    @Test
    public void shouldNotConnectToVpnIfSwitchIsOff() {
        doReturn(false).when(loginActivity).isSwitchOn(context);
        doReturn(false).when(vpnHelper).isVpnConnected();

        loginActivity.connectToVpn(context, vpnHelper);

        verify(loginActivity, times(0)).startVpnConnectionActivity(context);
    }
}