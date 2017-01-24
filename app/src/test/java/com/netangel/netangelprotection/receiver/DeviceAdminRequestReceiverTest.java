package com.netangel.netangelprotection.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;

import com.netangel.netangelprotection.NetAngelJunitSuite;
import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.util.Config;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class DeviceAdminRequestReceiverTest extends NetAngelJunitSuite {

    private DeviceAdminRequestReceiver receiver;

    @Mock
    private Context context;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;

    @Before
    public void setUp() {
        receiver = spy(new DeviceAdminRequestReceiver());

        when(context.getSharedPreferences("com_netangel_netangelprotection", Context.MODE_PRIVATE))
                .thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);

        when(context.getString(R.string.device_admin_description))
                .thenReturn("description string");

        doNothing().when(receiver).startMainActivity(context);
    }

    @Test
    public void shouldSaveEnabledConfiguration() {
        receiver.onEnabled(context, null);

        verify(editor).putBoolean(Config.IS_DEVICE_ADMIN, true);
        verify(editor).commit();
        verifyNoMoreInteractions(editor);
    }

    @Test
    public void shouldSaveDisabledConfiguration() {
        receiver.onDisabled(context, null);

        verify(editor).putBoolean(Config.IS_DEVICE_ADMIN, false);
        verify(editor).commit();
        verifyNoMoreInteractions(editor);
    }

    @Test
    public void shouldProvideDescription() {
        String result = receiver.onDisableRequested(context, null).toString();

        assertEquals("description string", result);
    }
}