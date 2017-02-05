package com.netangel.netangelprotection.ui;

import android.app.Activity;
import android.content.Context;

import com.netangel.netangelprotection.NetAngelJunitSuite;
import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.util.VpnHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class LoginActivityTest extends NetAngelJunitSuite {

    private LoginActivity loginActivity;

    @Mock
    private VpnHelper vpnHelper;
    @Mock
    private Context context;

    @Before
    public void setUp() {
        loginActivity = spy(new LoginActivity());
        doNothing().when(loginActivity).startVpnConnectionActivity(context);
        doNothing().when(loginActivity).alertIncorrectPassword(anyString());

        when(context.getString(R.string.enter_password_message)).thenReturn("Please enter password");
        when(context.getString(R.string.password_length_message)).thenReturn("Password has to be at least 8 characters");
        when(context.getString(R.string.enter_email_message)).thenReturn("Please enter email");
        when(context.getString(R.string.invalid_email_message)).thenReturn("Invalid email");

        doReturn(true).when(loginActivity).isValidEmail(anyString());
    }

    @Test
    public void shouldConnectToVpnWhenHelperSaysItIsNotConnected() {
        doReturn(true).when(loginActivity).isSwitchOn(context);
        doReturn(false).when(vpnHelper).isVpnConnected();

        loginActivity.connectToVpn(context, vpnHelper);

        verify(loginActivity).startVpnConnectionActivity(context);
    }

    @Test
    public void shouldNotConnectToVpnIfHelperSaysItIsOn() {
        doReturn(true).when(loginActivity).isSwitchOn(context);
        doReturn(true).when(vpnHelper).isVpnConnected();

        loginActivity.connectToVpn(context, vpnHelper);

        verify(loginActivity, times(0)).startVpnConnectionActivity(context);
    }

    @Test
    public void shouldNotConnectToVpnIfSwitchIsOff() {
        doReturn(false).when(loginActivity).isSwitchOn(context);
        doReturn(false).when(vpnHelper).isVpnConnected();

        loginActivity.connectToVpn(context, vpnHelper);

        verify(loginActivity, times(0)).startVpnConnectionActivity(context);
    }

    @Test
    public void shouldProvideErrorMessageIfPasswordIsEmpty() {
        String message = loginActivity.validatePassword(context, "");
        assertEquals("Please enter password", message);
    }

    @Test
    public void shouldProvideErrorMessageIfPasswordIsNull() {
        String message = loginActivity.validatePassword(context, null);
        assertEquals("Please enter password", message);
    }

    @Test
    public void shouldProvideErrorMessageIfPasswordIsTooShort() {
        String message = loginActivity.validatePassword(context, "abcd");
        assertEquals("Password has to be at least 8 characters", message);
    }

    @Test
    public void shouldValidatePassword() {
        String message = loginActivity.validatePassword(context, "abcdefgh");
        assertEquals(null, message);
    }

    @Test
    public void shouldProvideErrorMessageIfEmailIsEmpty() {
        String message = loginActivity.validateEmail(context, "");
        assertEquals("Please enter email", message);
    }

    @Test
    public void shouldProvideErrorMessageIfEmailIsNull() {
        String message = loginActivity.validateEmail(context, null);
        assertEquals("Please enter email", message);
    }

    @Test
    public void shouldProvideErrorMessageIfItIsNotAnEmail() {
        doReturn(false).when(loginActivity).isValidEmail(anyString());
        String message = loginActivity.validateEmail(context, "hey@luke");
        assertEquals("Invalid email", message);
    }

    @Test
    public void shouldValidateEmail() {
        String message = loginActivity.validateEmail(context, "hey@luke.com");
        assertEquals(null, message);
    }

    @Test
    public void shouldAlertOnIncorrectEmail() {
        doReturn("invalid email message").when(loginActivity).validateEmail(any(Context.class), anyString());
        doReturn(null).when(loginActivity).validatePassword(any(Context.class), anyString());

        boolean isValid = loginActivity.validateInput(context, "email", "password");

        assertFalse(isValid);
        verify(loginActivity).alertIncorrectPassword("invalid email message");
    }

    @Test
    public void shouldAlertOnIncorrectPassword() {
        doReturn(null).when(loginActivity).validateEmail(any(Context.class), anyString());
        doReturn("invalid password message").when(loginActivity).validatePassword(any(Context.class), anyString());

        boolean isValid = loginActivity.validateInput(context, "email", "password");

        assertFalse(isValid);
        verify(loginActivity).alertIncorrectPassword("invalid password message");
    }

    @Test
    public void incorrectEmailShouldOverrideIncorrectPasswordMessage() {
        doReturn("invalid email message").when(loginActivity).validateEmail(any(Context.class), anyString());
        doReturn("invalid password message").when(loginActivity).validatePassword(any(Context.class), anyString());

        boolean isValid = loginActivity.validateInput(context, "email", "password");

        assertFalse(isValid);
        verify(loginActivity).alertIncorrectPassword("invalid email message");
    }
}