package com.xstd.ip;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by michael on 13-12-23.
 */
public class BindFakeActivity extends Activity {

    private Handler mHandler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Tools.bindDeviceManager(BindFakeActivity.this);
            }
        }, 500);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000) {
            if (Tools.isBindingActive(getApplicationContext())) {
                finish();
                return;
            }

            Config.LEFT_ACTIVE_BUTTON.set(true);
            finish();
        }
    }
}