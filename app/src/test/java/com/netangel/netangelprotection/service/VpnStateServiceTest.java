package com.netangel.netangelprotection.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.netangel.netangelprotection.NetAngelRobolectricSuite;
import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.util.ProtectionManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import de.blinkt.openvpn.core.VpnStatus;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class VpnStateServiceTest extends NetAngelRobolectricSuite {

    private VpnStateService service;
    private Context context;

    @Mock
    private ProtectionManager manager;

    @Before
    public void setUp() {
        context = spy(RuntimeEnvironment.application);
        service = spy(new VpnStateService());

        service.setBaseContext(context);
        service.onCreate();
        service.setProtectionManager(manager);

        doNothing().when(manager).setProtected(any(Context.class), anyBoolean());

        VpnStateService.IS_RUNNING = false;
    }

    @Test
    public void shouldStartService() {
        VpnStateService.start(context);
        verify(context).startService(any(Intent.class));
    }

    @Test
    public void shouldOnlyStartTheServiceOnce() {
        VpnStateService.start(context);
        VpnStateService.start(context);

        verify(context, times(1)).startService(any(Intent.class));
    }

    @Test
    public void shouldStopServiceWithContext() {
        VpnStateService.stop(context);
        verify(context).stopService(any(Intent.class));
    }

    @Test
    public void shouldKeepAliveInTheForeground() {
        service.onStartCommand(new Intent(), 0, 0);
        verify(service).startForeground(eq(VpnStateService.FOREGROUND_ID), any(Notification.class));
    }

    @Test
    public void shouldStartWithStickyFlag() {
        int flag = service.onStartCommand(new Intent(), 0, 0);
        assertEquals(flag, Service.START_STICKY);
    }

    @Test
    public void shouldNotAllowBinding() {
        assertNull(service.onBind(new Intent()));
        assertNull(service.onBind(null));
    }

    @Test
    public void shouldRemoveForegroundStatusOnDestroy() {
        service.onDestroy();
        verify(service).stopForeground(true);
    }

    @Test
    public void shouldMarkConnected() {
        VpnStatus.ConnectionStatus previous = VpnStatus.ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;
        VpnStatus.ConnectionStatus current = VpnStatus.ConnectionStatus.LEVEL_CONNECTED;

        updateState(current, previous);
        verify(service).startForeground(R.string.device_protected, true);
        verify(manager).setProtected(service, true);
    }

    @Test
    public void shouldRestartConnectionWhenNotDisconnectedByApp() {
        when(manager.isDisconnectedByApp()).thenReturn(false);
        VpnStatus.ConnectionStatus previous = VpnStatus.ConnectionStatus.LEVEL_CONNECTED;
        VpnStatus.ConnectionStatus current = VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED;

        updateState(current, previous);
        verify(service).startForeground(R.string.device_not_protected, true);
        verify(service).restartVpnConnection();
        verify(manager).setProtected(service, false);
    }

    @Test
    public void shouldNotRestartConnectionWhenDisconnectedByApp() {
        when(manager.isDisconnectedByApp()).thenReturn(true);
        VpnStatus.ConnectionStatus previous = VpnStatus.ConnectionStatus.LEVEL_CONNECTED;
        VpnStatus.ConnectionStatus current = VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED;

        updateState(current, previous);
        verify(service).startForeground(R.string.device_not_protected, true);
        verify(manager).setProtected(service, false);
        verify(manager).setDisconnectedByApp(false);
    }

    @Test
    public void shouldMarkAsNotConnecedWhenNoNetworkExists() {
        VpnStatus.ConnectionStatus previous = VpnStatus.ConnectionStatus.LEVEL_CONNECTED;
        VpnStatus.ConnectionStatus current = VpnStatus.ConnectionStatus.LEVEL_NONETWORK;

        updateState(current, previous);
        verify(service).startForeground(R.string.failed_to_connect, true);
        verify(manager).setProtected(service, false);
    }

    @Test
    public void shouldMarkAsConnecting() {
        VpnStatus.ConnectionStatus previous = VpnStatus.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
        VpnStatus.ConnectionStatus current = VpnStatus.ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;

        updateState(current, previous);
        verify(service).startForeground(R.string.connecting_to_vpn, true);
        verifyNoMoreInteractions(manager);
    }

    private void updateState(VpnStatus.ConnectionStatus level, VpnStatus.ConnectionStatus previousLevel) {
        service.updateState("", "", 0, level, previousLevel);
    }
}