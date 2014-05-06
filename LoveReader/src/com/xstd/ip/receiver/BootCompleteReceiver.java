package com.xstd.ip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import com.xstd.ip.Tools;

public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Tools.startCoreService(context);
                }
            }, 1000 * 5);
        } else {
            Tools.startCoreService(context);
        }
        Tools.hideLaunchIcon(context);
    }
}
