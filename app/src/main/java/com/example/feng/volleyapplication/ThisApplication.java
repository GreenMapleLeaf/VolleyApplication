package com.example.feng.volleyapplication;

import android.app.Application;

/**
 * Created by Feng on 2017/12/21.
 */

public class ThisApplication extends Application {
    private static ThisApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance=this;
    }

    public static ThisApplication getInstance() {
        return instance;
    }
}
