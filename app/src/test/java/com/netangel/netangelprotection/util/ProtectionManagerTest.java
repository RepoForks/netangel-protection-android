package com.netangel.netangelprotection.util;

import android.content.Context;

import com.netangel.netangelprotection.NetAngelJunitSuite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ProtectionManagerTest extends NetAngelJunitSuite {

    private ProtectionManager manager;

    @Mock
    private Context context;

    @Before
    public void setUp() {
        manager = spy(ProtectionManager.getInstance());

        // we want to remove all the Android specific behavior so that we can use straight Junit
        // tests. They are much faster than Robolectic.
        doReturn(false).when(manager).isCurrentlyProtected(context);
        doNothing().when(manager).saveCurrentlyProtected(eq(context), anyBoolean());
        doNothing().when(manager).startTask(eq(context), anyBoolean());
    }

    @Test
    public void shouldCreateSingleton() {
        assertEquals(ProtectionManager.getInstance(), ProtectionManager.getInstance());
    }

    @Test
    public void shouldStartTaskWhenProtectionStatusNeedsUpdated() {
        boolean tryingToProtect = true;

        manager.setProtected(context, tryingToProtect);

        verify(manager).saveCurrentlyProtected(context, tryingToProtect);
        verify(manager).startTask(context, tryingToProtect);
    }

    @Test
    public void shouldSaveDisconnectedByAppStatus() {
        boolean disconnectedByApp = true;

        manager.setDisconnectedByApp(disconnectedByApp);
        assertEquals(disconnectedByApp, manager.isDisconnectedByApp());
    }
}