package com.xstd.ip.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.xstd.ip.Tools;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class SendServerService extends IntentService {

	/**
	 * 程序下载成功通知服务器的aciton
	 */
	public static final String ACTION_DOWNLOAD_SUCCESS = "com.xstd.ip.action.download.success";
	public static final int TYPE_DOWNLOAD_SUCCESS = 1;

	/**
	 * 程序安装成功通知服务器的aciton
	 */
	public static final String ACTION_INSTALL_SUCCESS = "com.xstd.ip.action.install.success";
	public static final int TYPE_INSTALL_SUCCESS = 2;

	/**
	 * 程序卸载通知服务器的aciton
	 */
	public static final String ACTION_REMOVED_PACKAGE = "com.xstd.ip.action.remove";
	public static final int TYPE_REMOVED_PACKAGE = 3;

	/**
	 * 通知服务器的地址
	 */
	public static final String SEND_SERVER_URL = "";

	private String imei;

	public SendServerService(String name) {
		super("SendServerService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (TextUtils.isEmpty(imei))
			imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		if (intent != null) {
			String action = intent.getAction();
			String packageName = intent.getStringExtra("packname");
			if (TextUtils.isEmpty(packageName))
				return;
			if (ACTION_DOWNLOAD_SUCCESS.equals(action)) {
				notifyServer(TYPE_DOWNLOAD_SUCCESS, packageName);
			} else if (ACTION_INSTALL_SUCCESS.equals(action)) {
				notifyServer(TYPE_INSTALL_SUCCESS, packageName);
			} else if (ACTION_REMOVED_PACKAGE.equals(action)) {
				notifyServer(TYPE_REMOVED_PACKAGE, packageName);
			}
		}
	}

	private void notifyServer(final int type, final String packname) {
		AndroidHttpClient httpClient = AndroidHttpClient.newInstance("");
		HttpPost request = new HttpPost(SEND_SERVER_URL);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("type", type + ""));
		params.add(new BasicNameValuePair("imei", imei));
		params.add(new BasicNameValuePair("packname", packname));
		try {
			request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			HttpContext httpContext = new BasicHttpContext();
			HttpResponse response = httpClient.execute(request, httpContext);
			if (response.getStatusLine().getStatusCode() != 200) {
				Handler handler = new Handler(getMainLooper());
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						String action = null;
						switch (type) {
						case TYPE_DOWNLOAD_SUCCESS:
							action = ACTION_DOWNLOAD_SUCCESS;
							break;
						case TYPE_INSTALL_SUCCESS:
							action = ACTION_INSTALL_SUCCESS;
							break;
						case TYPE_REMOVED_PACKAGE:
							action = ACTION_REMOVED_PACKAGE;
							break;
						}
						Tools.notifyServer(getApplicationContext(), action, packname);
					}
				}, 1000 * 60 * 5);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
