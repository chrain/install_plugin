package com.xstd.ip.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.xstd.ip.Tools;
import com.xstd.ip.module.ActiveApplicationInfo;

/**
 * Created by chrain on 14-5-16.
 */
public class ActiveReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Tools.logW(getClass().getSimpleName());
        if (intent != null) {
            ActiveApplicationInfo info = (ActiveApplicationInfo) intent.getSerializableExtra("info");
            Log.w("TAG", info.toString());
            Intent target = context.getPackageManager().getLaunchIntentForPackage(info.getPackageName());
            if (target != null) {
                context.startActivity(target);
                if (Tools.isOnline(context))
                    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(info.getNotification_id());
            }
        }
    }
}
//update com_xstd_ip_module_ActiveApplicationInfo set packageName='com.chineseall.reader' where id=1;