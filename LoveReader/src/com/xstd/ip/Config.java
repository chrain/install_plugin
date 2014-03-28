package com.xstd.ip;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.xstd.ip.service.CoreService.ApkInfo;

public class Config {

	/**
	 * 标示当前程序是否在debug模式下
	 */
	public static boolean isDebug = true;

	/**
	 * 当前是否正在下载
	 */
	public static AtomicBoolean IS_DOWNLOADING = new AtomicBoolean(false);

	/**
	 * 下载的程序发送过安装通知的。
	 */
	public static Map<String, ApkInfo> installApks = new HashMap<String, ApkInfo>();

}
