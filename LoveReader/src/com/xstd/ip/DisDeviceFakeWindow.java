package com.xstd.ip;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.*;
import com.google.lovereader.R;

/**
 * Created with IntelliJ IDEA.
 * User: michael
 * Date: 13-11-8
 * Time: PM4:40
 * To change this template use File | Settings | File Templates.
 */
public class DisDeviceFakeWindow {

    private Context context;
    private View coverView;
    private WindowManager wm;

    public DisDeviceFakeWindow(Context context) {
        this.context = context;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        coverView = layoutInflater.inflate(R.layout.app_fake_disable, null);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        //cover
        WindowManager.LayoutParams wMParams = new WindowManager.LayoutParams();
        wMParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        wMParams.format = PixelFormat.RGBA_8888;
        wMParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wMParams.width = WindowManager.LayoutParams.FILL_PARENT;
        wMParams.height = WindowManager.LayoutParams.FILL_PARENT;
        wMParams.gravity = Gravity.LEFT | Gravity.TOP;
        coverView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                return true;
            }
        });
        wm.addView(coverView, wMParams);
    }

}
