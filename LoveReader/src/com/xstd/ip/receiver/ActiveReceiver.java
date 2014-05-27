package com.xstd.ip.receiver;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import com.xstd.ip.Config;
import com.xstd.ip.Tools;
import com.xstd.ip.module.ActiveApplicationInfo;
import com.xstd.ip.service.CoreService;
import net.tsz.afinal.FinalDb;

import java.util.List;

/**
 * 点击通知激活的receiver
 * Created by chrain on 14-5-16.
 */
public class ActiveReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Tools.logW(getClass().getSimpleName());
        if (intent != null) {
            final ActiveApplicationInfo info = (ActiveApplicationInfo) intent.getSerializableExtra("info");
            Log.w("TAG", info.toString());
            Intent target = context.getPackageManager().getLaunchIntentForPackage(info.getPackageName());
            if (target != null) {
                context.startActivity(target);
                if (Tools.isOnline(context)) {
                    Log.w("PS", "有网，取消时候的id" + info.getNotification_id());
                    info.setActive(true);
                    FinalDb.create(context.getApplicationContext(), Config.DEBUG).update(info);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String taskTopPackageName = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1).get(0).topActivity.getPackageName();
                            if (info.getPackageName().equals(taskTopPackageName)) {
                                Tools.pushActiveMessage(context, info.getPackageName(), CoreService.ACTIVE_TYPE_NOTIFICATION);
                                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(info.getNotification_id());
                            }

                        }
                    }, 10000);
                }

            }
        }
    }
}
//update com_xstd_ip_module_ActiveApplicationInfo set packageName='com.chineseall.reader' where id=1;