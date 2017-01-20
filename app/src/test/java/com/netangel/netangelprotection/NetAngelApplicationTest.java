package com.netangel.netangelprotection;

import org.hamcrest.core.IsNot;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

public class NetAngelApplicationTest extends NetAngelRobolectricSuite {

    @Test
    public void shouldCreateWithoutError() {
        RuntimeEnvironment.application.onCreate();
    }

    @Test
    public void applicationShouldNotBeNull() {
        assertNotNull(RuntimeEnvironment.application);
    }
}
