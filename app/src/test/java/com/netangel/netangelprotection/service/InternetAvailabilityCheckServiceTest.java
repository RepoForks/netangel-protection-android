package com.netangel.netangelprotection.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import com.netangel.netangelprotection.NetAngelRobolectricSuite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InternetAvailabilityCheckServiceTest extends NetAngelRobolectricSuite {

    private InternetAvailabilityCheckService service;
    private Context context;

    @Mock
    private AlarmManager manager;

    @Before
    public void setUp() throws IOException {
        context = spy(RuntimeEnvironment.application);
        service = spy(Robolectric.buildService(InternetAvailabilityCheckService.class).create().get());

        when(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(manager);
    }

    @Test
    public void shouldScheduleServiceWithAlarmManager() {
        InternetAvailabilityCheckService.scheduleNextRun(context);

        verify(manager).cancel(any(PendingIntent.class));
        verify(manager).setExact(anyInt(), anyLong(), any(PendingIntent.class));
    }

    @Test
    public void shouldDoNothingIfSwitchIsOff() {
        when(service.isSwitchOn()).thenReturn(false);
        service.onHandleIntent(null);

        verify(service, times(0)).restartVpnConnection();
    }

    @Test
    public void shouldDoNothingIfInternetIsAvailable() {
        when(service.isSwitchOn()).thenReturn(true);
        doReturn(true).when(service).hasConnection();
        service.onHandleIntent(null);

        verify(service, times(0)).restartVpnConnection();
    }

    @Test
    public void shouldRestartVpnIfInternetIsUnavailable() {
        when(service.isSwitchOn()).thenReturn(true);
        doReturn(false).when(service).hasConnection();
        service.onHandleIntent(null);

        verify(service).restartVpnConnection();
    }
}