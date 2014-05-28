package com.xstd.ip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.xstd.ip.Config;
import com.xstd.ip.InitApplication;
import com.xstd.ip.Tools;
import com.xstd.ip.module.ApplicationInfo;
import com.xstd.ip.module.PushMessage;
import com.xstd.ip.service.SendServerService;
import net.tsz.afinal.FinalDb;

import java.util.List;

public class PackageAddRemReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getDataString().substring(8);
        InitApplication application = ((InitApplication) context.getApplicationContext());
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            List<ApplicationInfo> infos = application.getFinalDb().findAll(ApplicationInfo.class);
            if (infos != null && infos.size() > 0) {
                for (ApplicationInfo info : infos) {
                    if (info.getPackageName().equalsIgnoreCase(packageName)) {
                        info.setInstall(true);
                        info.setInstallTime(System.currentTimeMillis());
                        application.getFinalDb().update(info);
                        PushMessage message = new PushMessage();
                        message.setPackageName(packageName);
                        message.setToken(info.getToken());
                        message.setType(SendServerService.TYPE_INSTALL_SUCCESS);
                        application.getFinalDb().save(message);
                        if (!info.isSilence()) {
                            Tools.cancleNotification(context, packageName);
                        }
//                        Tools.launchApplication(context, packageName);
                    }
                }
            }
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            List<ApplicationInfo> infos = application.getFinalDb().findAllByWhere(ApplicationInfo.class, String.format("packageName='%s'", packageName));
            if (infos != null && infos.size() > 0) {
                PushMessage message = new PushMessage();
                message.setPackageName(packageName);
                message.setToken(infos.get(0).getToken());
                message.setType(SendServerService.TYPE_REMOVED_PACKAGE);
                ((InitApplication) context.getApplicationContext()).getFinalDb().save(message);
            }
        }
    }
}
