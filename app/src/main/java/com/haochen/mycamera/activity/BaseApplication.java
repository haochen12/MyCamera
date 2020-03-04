package com.haochen.mycamera.activity;

import android.app.Application;

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThreadPoolConfig mThreadPoolConfig = new ThreadPoolConfig.Builder()
                .setThreadNum(1)
                .setPriority(1)
                .build();
        MyThreadPool.getInstance().init(mThreadPoolConfig);
    }
}
