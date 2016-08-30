package com.netangel.netangelprotection.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.netangel.netangelprotection.NetAngelApplication;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class Auth {

	private Auth() {}

	public static String generateHeader(String method, String path) {
		Context c = NetAngelApplication.getAppContext();
		String secretToken = Config.getString(c, Config.SECRET_TOKEN);
		String certificate = Config.getString(c, Config.SSL_CERT);
		return generateHeader(certificate, secretToken, method, path);
	}

	public static String generateHeader(String certificate, String secretToken, String method, String path) {
		long timestamp = System.currentTimeMillis() / 1000L;
		String clientNonce = UUID.randomUUID().toString().replaceAll("-", "");
		String authString = certificate + "&" + timestamp + "&" + clientNonce + "&" + method + "&" + path;
		String signature = hmacDigest(authString, secretToken, "HmacSHA256");
		String data = "certificate=\"" + certificate + "\", cnonce=\"" + clientNonce + "\", signature=\"" + signature + "\", timestamp=\""
			+ timestamp + "\"";
		String encodedData = Base64.encodeToString(data.getBytes(), Base64.NO_WRAP);
		return "Certificate " + encodedData;
	}

	private static String hmacDigest(String msg, String keyString, String algo) {
		String digest = null;
		try {
			SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
			Mac mac = Mac.getInstance(algo);
			mac.init(key);
			byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));
			StringBuilder hash = new StringBuilder();
			for (byte aByte : bytes) {
				String hex = Integer.toHexString(0xFF & aByte);
				if (hex.length() == 1) {
					hash.append('0');
				}
				hash.append(hex);
			}
			digest = hash.toString();
		} catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException e) {
			e.printStackTrace();
		}
		return digest;
	}
}