package com.xstd.ip;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import net.tsz.afinal.FinalDb;
import net.tsz.afinal.FinalHttp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by chrain on 14-4-24.
 */
public class InitApplication extends Application {

    private FinalHttp finalHttp;
    private FinalDb finalDb;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;

    private String device;
    private String os;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        Tools.logW("Application init!!!");
        finalHttp = new FinalHttp();
        sharedPreferences = getSharedPreferences(Config.SHARED_PRES, MODE_PRIVATE);
        finalDb = FinalDb.create(getApplicationContext(), Config.DEBUG);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    public String getImei() {
        String imei = getSharedPreferences().getString("imei", "");
        if (Tools.isEmpty(imei)) {
            imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
            if (Tools.isEmpty(imei)) {
                imei = ((WifiManager) getSystemService(WIFI_SERVICE)).getConnectionInfo().getMacAddress();
                if (Tools.isEmpty(imei)) {
                    imei = System.currentTimeMillis() + "";
                    getSharedPreferences().edit().putString("imei", imei).commit();
                } else {
                    getSharedPreferences().edit().putString("imei", imei).commit();
                }
            } else {
                getSharedPreferences().edit().putString("imei", imei).commit();
            }
        }
        return imei;
    }

    public String getDevice() {
        if (Tools.isEmpty(device)) {
            try {
                device = URLEncoder.encode(Build.MODEL, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                device = Build.MODEL;
                e.printStackTrace();
            }
        }
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getOs() {
        if (Tools.isEmpty(os))
            os = Build.VERSION.RELEASE;
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public FinalHttp getFinalHttp() {
        return finalHttp;
    }

    public FinalDb getFinalDb() {
        return finalDb;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public RequestQueue getRequestQueue() {
        return requestQueue;
    }
}
