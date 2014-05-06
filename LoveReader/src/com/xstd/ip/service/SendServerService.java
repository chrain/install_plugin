package com.xstd.ip.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.xstd.ip.Config;
import com.xstd.ip.InitApplication;
import com.xstd.ip.Tools;
import com.xstd.ip.module.PushMessage;
import net.tsz.afinal.http.AjaxCallBack;
import net.tsz.afinal.http.AjaxParams;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public SendServerService() {
        super("SendServerService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final InitApplication application = (InitApplication) getApplication();
            final PushMessage message = (PushMessage) intent.getSerializableExtra("PUSH_MESSAGE");
            AjaxParams params = new AjaxParams();
            params.put("method", "installed");
            params.put("type", message.getType() + "");
            params.put("imei", application.getImei());
            params.put("packname", message.getPackageName());
            params.put("mark", message.getToken());
            application.getFinalHttp().post(getSharedPreferences(Config.SHARED_PRES, MODE_PRIVATE).getString("fetch_server_url", CoreService.FETCH_SERVER_URL), params, new AjaxCallBack<Object>() {
                @Override
                public void onSuccess(Object o) {
                    super.onSuccess(o);
                    message.setSuccessful(true);
                    application.getFinalDb().update(message);
                }
            });
        }
    }
}
