package com.netangel.netangelprotection.util;

import android.content.Context;

import com.netangel.netangelprotection.NetAngelRobolectricSuite;
import com.netangel.netangelprotection.R;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;

public class DeviceTest extends NetAngelRobolectricSuite {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
    }

    @Test
    public void shouldFindScreenSizeResourceForPhone() {
        assertEquals("phone", context.getString(R.string.device_type));
    }

    @Test
    @org.robolectric.annotation.Config(qualifiers="sw600dp")
    public void shouldFindScreenSizeResourcesForTablet() {
        assertEquals("tablet", context.getString(R.string.device_type));
    }
}