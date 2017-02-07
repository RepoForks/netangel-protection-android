package com.netangel.netangelprotection.util;

import android.content.Context;

import com.netangel.netangelprotection.NetAngelJunitSuite;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class VpnHelperTest extends NetAngelJunitSuite {

    private VpnHelper helper;

    @Mock
    private Context context;

    @Before
    public void setUp() {
        helper = spy(new VpnHelper(context));
    }

    @Test
    public void shouldMarkConnectedForTunConnection() throws SocketException {
        doReturn(generateInterfaces("tun0", "wlan0", "dummy0")).when(helper).getNetworkInterfaces();
        assertTrue(helper.isVpnConnected());
    }

    @Test
    public void shouldMarkConnectedForPptpConnection() throws SocketException {
        doReturn(generateInterfaces("pptp0", "wlan0", "dummy0")).when(helper).getNetworkInterfaces();
        assertTrue(helper.isVpnConnected());
    }

    @Test
    public void shouldNotMarkConnectedforWlanConnection() throws SocketException {
        doReturn(generateInterfaces("wlan0", "dummy0")).when(helper).getNetworkInterfaces();
        assertFalse(helper.isVpnConnected());
    }

    @Test
    public void shouldNotMarkConnectedOnSocketException() throws SocketException {
        doThrow(new SocketException("fail trying to find connections")).when(helper).getNetworkInterfaces();
        assertFalse(helper.isVpnConnected());
    }

    private List<VpnHelper.NetworkInterfaceWrapper> generateInterfaces(String ... names) throws SocketException {
        List<VpnHelper.NetworkInterfaceWrapper> interfaces = new ArrayList<>();

        for (String s : names) {
            VpnHelper.NetworkInterfaceWrapper networkInterface =
                    new VpnHelper.NetworkInterfaceWrapper(s, true);

            interfaces.add(networkInterface);
        }

        return interfaces;
    }
}