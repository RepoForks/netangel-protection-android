package com.netangel.netangelprotection.receiver;

import android.content.Context;
import android.content.Intent;

import com.netangel.netangelprotection.NetAngelJunitSuite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class BootReceiverTest extends NetAngelJunitSuite {

    private BootReceiver bootReceiver;

    @Mock
    private Context context;
    @Mock
    private Intent intent;

    @Before
    public void setUp() {
        bootReceiver = spy(new BootReceiver());
        doNothing().when(bootReceiver).startVpnConnection(context);
        doReturn(true).when(bootReceiver).isSwitchOn(context);
    }

    @Test
    public void shouldConnectOnBoot() {
        when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);
        bootReceiver.onReceive(context, intent);

        verify(bootReceiver).startVpnConnection(context);
    }

    @Test
    public void shouldConnectOnReinstall() {
        when(intent.getAction()).thenReturn(Intent.ACTION_MY_PACKAGE_REPLACED);
        bootReceiver.onReceive(context, intent);

        verify(bootReceiver).startVpnConnection(context);
    }

    @Test
    public void shouldNotConnectForOtherIntents() {
        when(intent.getAction()).thenReturn(Intent.ACTION_BATTERY_CHANGED);
        bootReceiver.onReceive(context, intent);

        verify(bootReceiver, times(0)).startVpnConnection(context);
    }

    @Test
    public void shouldNotConnectIfSwitchIsOff() {
        when(intent.getAction()).thenReturn(Intent.ACTION_BOOT_COMPLETED);
        doReturn(false).when(bootReceiver).isSwitchOn(context);

        bootReceiver.onReceive(context, intent);

        verify(bootReceiver, times(0)).startVpnConnection(context);
    }
}