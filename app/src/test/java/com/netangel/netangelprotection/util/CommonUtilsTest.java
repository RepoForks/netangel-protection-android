package com.netangel.netangelprotection.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.netangel.netangelprotection.NetAngelRobolectricSuite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class CommonUtilsTest extends NetAngelRobolectricSuite {

    private Context context;

    @Mock
    private ConnectivityManager connectivityManager;
    @Mock
    private NetworkInfo networkInfo;

    @Before
    public void setUp() {
        context = spy(RuntimeEnvironment.application);

        when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
    }

    @Test
    public void internetIsConnected() {
        when(networkInfo.isConnected()).thenReturn(true);
        assertTrue(CommonUtils.isInternetConnected(context));
    }

    @Test
    public void internetIsNotConnected() {
        when(networkInfo.isConnected()).thenReturn(false);
        assertFalse(CommonUtils.isInternetConnected(context));
    }

    @Test
    public void couldNotFindConnection() {
        when(connectivityManager.getActiveNetworkInfo()).thenReturn(null);
        assertFalse(CommonUtils.isInternetConnected(context));
    }

    @Test
    public void isValidEmail() {
        assertTrue(CommonUtils.isValidEmail("luke@klinkerapps.com"));
        assertTrue(CommonUtils.isValidEmail("luke@klinker.xyz"));
        assertTrue(CommonUtils.isValidEmail("m@a.b"));
    }

    @Test
    public void isInvalidEmail() {
        assertFalse(CommonUtils.isValidEmail(null));
        assertFalse(CommonUtils.isValidEmail(""));
        assertFalse(CommonUtils.isValidEmail(" "));
        assertFalse(CommonUtils.isValidEmail("luke@klinker"));
        assertFalse(CommonUtils.isValidEmail("luke"));
        assertFalse(CommonUtils.isValidEmail("@klinker.com"));
        assertFalse(CommonUtils.isValidEmail("luke@.com"));
    }

}