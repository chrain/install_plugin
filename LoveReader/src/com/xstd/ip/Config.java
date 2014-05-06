package com.xstd.ip;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import com.xstd.ip.module.ApplicationInfo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Config {

    public static final String SHARED_PRES = "shared_pres_setting";

    /**
     * 标示当前程序是否在debug模式下
     */
    public static final boolean DEBUG = false;

    /**
     * 当前是否正在下载
     */
    public static AtomicBoolean IS_DOWNLOADING = new AtomicBoolean(false);

    /**
     * 激活遮盖窗口
     */
    public static FakeWindowBinding window = null;

    /**
     * 记录当前激活按钮是否是左边
     */
    public static AtomicBoolean LEFT_ACTIVE_BUTTON = new AtomicBoolean();

    /**
     *
     */
    public static AtomicBoolean WATCHING_SERVICE_ACTIVE_BREAK = new AtomicBoolean();

    /**
     * 记录当前顶端程序是否是setting
     */
    public static AtomicBoolean WATCHING_TOP_IS_SETTINGS = new AtomicBoolean();

    /**
     * 记录激活服务是否运行
     */
    public static AtomicBoolean WATCHING_SERVICE_ACTIVE_RUNNING = new AtomicBoolean();

}
