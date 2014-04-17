package com.xstd.ip;

import android.app.Application;

/**
 * Created by Chrain on 2014/4/14.
 */
public class CrashApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(getApplicationContext());
    }
}
