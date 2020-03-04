package com.haochen.mycamera.activity;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyThreadPool {

    public static ExecutorService mExecutorService;
    private static MyThreadPool INSTANCE;

    public static MyThreadPool getInstance() {
        if (Objects.isNull(INSTANCE)) {
            INSTANCE = new MyThreadPool();
        }
        return INSTANCE;
    }

    void init(ThreadPoolConfig threadPoolConfig) {
        if (Objects.isNull(mExecutorService)) {
            mExecutorService = Executors.newFixedThreadPool(threadPoolConfig.threadNum);
        }
    }

    public ExecutorService getExecutor() {
        return mExecutorService;
    }
}
