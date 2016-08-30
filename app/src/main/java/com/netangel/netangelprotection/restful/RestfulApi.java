package com.netangel.netangelprotection.restful;

import com.netangel.netangelprotection.BuildConfig;
import com.netangel.netangelprotection.NetAngelApplication;
import com.netangel.netangelprotection.R;
import com.netangel.netangelprotection.model.CheckInResult;
import com.netangel.netangelprotection.util.Auth;
import com.netangel.netangelprotection.util.Config;
import com.netangel.netangelprotection.util.Device;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class RestfulApi {

	private static final String CLIENT_ID = "client_id";
	private static final String NAME = "name";
	private static final String DEVICE_TYPE = "device_type";
	private static final String MODEL_NAME = "model_name";
	private static final String MODEL_NUMBER = "model_number";
	private static final String OS = "os";
	private static final String VERSION = "version";

	public static RestfulApi instance;

	private RestService restService;
	private static Retrofit retrofit;

	public static RestfulApi getInstance() {
		if (instance == null) {
			instance = new RestfulApi();
		}
		return instance;
	}

	private RestfulApi() {
		String baseUrl = NetAngelApplication.getAppContext().getString(R.string.BASE_URL);
		HttpLoggingInterceptor hli = new HttpLoggingInterceptor();
		hli.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
		OkHttpClient client = new OkHttpClient.Builder()
				.connectTimeout(30, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS)
				.writeTimeout(30, TimeUnit.SECONDS)
				.addInterceptor(hli)
				.build();
		retrofit = new Retrofit.Builder()
				.baseUrl(baseUrl)
				.client(client)
				.addConverterFactory(GsonConverterFactory.create())
				.build();
		restService = retrofit.create(RestService.class);
	}

	public static Retrofit getRetrofit() {
		return retrofit;
	}

	public Call<ResponseBody> login(String email, String password) {

		String credential = Credentials.basic(email, password);
		return login(credential);
	}

	public Call<ResponseBody> login(String credential) {

		String clientId = Config.getString(NetAngelApplication.getAppContext(), Config.CLIENT_ID);

		HashMap<String, String> args = new HashMap<>();
		args.put(CLIENT_ID, clientId);
		args.put(NAME, Device.getName());
		args.put(DEVICE_TYPE, "phone");
		args.put(MODEL_NAME, Device.getName());
		args.put(MODEL_NUMBER, Device.getModel());
		args.put(OS, Device.getOS());
		args.put(VERSION, Device.getOSVersion());

		return restService.login(credential, args);
	}

	public Call<Boolean> isVpnUserCreated() {
		String clientId = Config.getString(NetAngelApplication.getAppContext(), Config.CLIENT_ID);
		String authorization = Auth.generateHeader("GET", "/api/v1/devices/" + clientId + "/vpn_user_created");
		return restService.isVpnUserCreated(authorization, clientId);
	}

	public Call<ResponseBody> downloadOvpnFile() {
		String clientId = Config.getString(NetAngelApplication.getAppContext(), Config.CLIENT_ID);
		String authorization = Auth.generateHeader("GET", "/api/v1/devices/" + clientId + "/download_ovpn");
		return restService.downloadOvpnFile(authorization, clientId);
	}

	public Call<CheckInResult> checkIn() {
		String clientId = Config.getString(NetAngelApplication.getAppContext(), Config.CLIENT_ID);
		String credential = Auth.generateHeader("POST", "/api/v1/devices/" + clientId + "/check_in");
		return restService.checkIn(credential, clientId);
	}

	public Call<ResponseBody> setProtected() {
		String clientId = Config.getString(NetAngelApplication.getAppContext(), Config.CLIENT_ID);
		String authorization = Auth.generateHeader("POST", "/api/v1/devices/" + clientId + "/protected");
		return restService.setProtected(authorization, clientId);
	}

	public Call<ResponseBody> setUnprotected() {
		String clientId = Config.getString(NetAngelApplication.getAppContext(), Config.CLIENT_ID);
		String authorization = Auth.generateHeader("POST", "/api/v1/devices/" + clientId + "/unprotected");
		return restService.setUnprotected(authorization, clientId);
	}

	// Retrofit interface
	private interface RestService {
		@Headers({
			"Accept: application/json",
			"Content-Type: application/json"
		})
		@POST("/api/v1/devices")
		Call<ResponseBody> login(@Header("Authorization") String authorization, @Body HashMap<String, String> body);

		@Headers({
				"Accept: application/json",
				"Content-Type: application/json"
		})
		@GET("/api/v1/devices/{client_id}/vpn_user_created")
		Call<Boolean> isVpnUserCreated(@Header("Authorization") String authorization,
									   @Path("client_id") String clientId);

		@Headers({
				"Accept: application/json",
				"Content-Type: application/json"
		})
		@GET("/api/v1/devices/{client_id}/download_ovpn")
		Call<ResponseBody> downloadOvpnFile(@Header("Authorization") String authorization,
											@Path("client_id") String clientId);

		@Headers({
			"Accept: application/json",
			"Content-Type: application/json"
		})
		@POST("/api/v1/devices/{client_id}/check_in")
		Call<CheckInResult> checkIn(@Header("Authorization") String authorization, @Path("client_id") String client_id);

		@Headers({
				"Accept: application/json",
				"Content-Type: application/json"
		})
		@POST("/api/v1/devices/{client_id}/protected")
		Call<ResponseBody> setProtected(@Header("Authorization") String authorization,
										@Path("client_id") String clientId);

		@Headers({
				"Accept: application/json",
				"Content-Type: application/json"
		})
		@POST("/api/v1/devices/{client_id}/unprotected")
		Call<ResponseBody> setUnprotected(@Header("Authorization") String authorization,
										  @Path("client_id") String clientId);
	}
}
