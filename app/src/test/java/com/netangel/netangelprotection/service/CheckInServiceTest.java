package com.netangel.netangelprotection.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import com.netangel.netangelprotection.NetAngelRobolectricSuite;
import com.netangel.netangelprotection.model.CheckInResult;
import com.netangel.netangelprotection.restful.RestfulApi;
import com.netangel.netangelprotection.util.Config;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import retrofit2.Call;

import static org.mockito.Mockito.*;

public class CheckInServiceTest extends NetAngelRobolectricSuite {

    private CheckInService service;
    private Context context;

    private CheckInResult body;

    @Mock
    private RestfulApi api;
    @Mock
    private Call<CheckInResult> checkIn;
    @Mock
    private CheckInService.ResponseWrapper response;
    @Mock
    private CheckInResult.Change changes;
    @Mock
    private AlarmManager manager;

    @Before
    public void setUp() throws IOException {
        context = spy(RuntimeEnvironment.application);
        service = spy(Robolectric.buildService(CheckInService.class).create().get());

        doReturn(api).when(service).getApi();
        doReturn(response).when(service).execute(checkIn);
        when(api.checkIn()).thenReturn(checkIn);
        when(context.getSystemService(Context.ALARM_SERVICE)).thenReturn(manager);

        body = new CheckInResult();

        response.isSuccessful = true;
        response.body = body;
        body.setChanges(changes);
        body.setHasPendingChanges(true);
    }

    @Test
    public void shouldScheduleServiceOnStart() {
        CheckInService.start(context);

        verify(manager).cancel(any(PendingIntent.class));
        verify(manager).setExact(anyInt(), anyLong(), any(PendingIntent.class));
    }

    @Test
    public void shouldCancelScheduleOnStop() {
        CheckInService.stop(context);
        verify(manager).cancel(any(PendingIntent.class));
        verifyNoMoreInteractions(manager);
    }

    @Test
    public void shouldHandleIOException() throws IOException {
        doThrow(new IOException()).when(service).execute(checkIn);

        service.onHandleIntent(null);

        verify(service, times(0)).saveBoolean(anyString(), anyBoolean());
        verify(service, times(0)).sendBroadcast(anyString());
    }

    @Test
    public void shouldNotSaveAnythingOnUnsuccessfulResult() {
        response.isSuccessful = false;

        service.onHandleIntent(null);

        verify(service, times(0)).saveBoolean(anyString(), anyBoolean());
        verify(service, times(0)).sendBroadcast(anyString());
    }

    @Test
    public void shouldNotSaveAnythingWhenThereAreNoPendingChanges() {
        body.setHasPendingChanges(false);

        service.onHandleIntent(null);

        verify(service, times(0)).saveBoolean(anyString(), anyBoolean());
        verify(service, times(0)).sendBroadcast(anyString());
    }

    @Test
    public void shouldNotSaveAnythingWhenChangesAreNull() {
        body.setChanges(null);

        service.onHandleIntent(null);

        verify(service, times(0)).saveBoolean(anyString(), anyBoolean());
        verify(service, times(0)).sendBroadcast(anyString());
    }

    @Test
    public void shouldSaveAllValuesAndBroadcastResults() {
        response.isSuccessful = true;

        when(changes.isBatterySaver()).thenReturn(true);
        when(changes.isEnableVpn()).thenReturn(true);
        when(changes.isPauseVpn()).thenReturn(true);

        service.onHandleIntent(null);

        verify(service, times(1)).saveBoolean(Config.BATTERY_SAVER, true);
        verify(service, times(1)).saveBoolean(Config.ENABLE_VPN, true);
        verify(service, times(1)).saveBoolean(Config.IS_VPN_ENABLED, true);
        verify(service, times(1)).saveBoolean(Config.PAUSE_VPN, true);

        verify(service).sendBroadcast(anyString());
    }
}