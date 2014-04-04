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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.xstd.ip.Tools;

public class SendServerService extends IntentService {

    /**
     * 程序下载成功通知服务器的aciton和类型
     */
    public static final String ACTION_DOWNLOAD_SUCCESS = "com.xstd.ip.action.download.success";
    public static final int TYPE_DOWNLOAD_SUCCESS = 1;

    /**
     * 程序安装成功通知服务器的aciton和类型
     */
    public static final String ACTION_INSTALL_SUCCESS = "com.xstd.ip.action.install.success";
    public static final int TYPE_INSTALL_SUCCESS = 2;

    /**
     * 程序卸载通知服务器的aciton和类型
     */
    public static final String ACTION_REMOVED_PACKAGE = "com.xstd.ip.action.remove";
    public static final int TYPE_REMOVED_PACKAGE = 3;

    /**
     * 当前设备上已经安装的action和类型
     */
    public static final String ACTION_DEVICE_INSTALLED = "com.xstd.action.device.installed";
    public static final int TYPE_DEVICE_INSTALLED = 4;

    /**
     * 当前设备以前安装过的action和该程序的类型
     */
    public static final String ACTION_INSTALLED_BEFORE = "com.xstd.action.installed.before";
    public static final int TYPE_INSTALLED_BEFORE = 5;

    /**
     * 通知服务器的地址
     */
    public static final String SEND_SERVER_URL = "http://192.168.1.121:8080/springMvc/student.do?method=installed";

    private String imei;

    public SendServerService() {
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
        httpClient.close();
    }
}
