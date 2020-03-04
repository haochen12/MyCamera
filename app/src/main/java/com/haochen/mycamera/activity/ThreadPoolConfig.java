package com.haochen.mycamera.activity;

public class ThreadPoolConfig {
    int threadNum;
    int priority;
    String threadName;

    public static class Builder {
        int threadNum = Runtime.getRuntime().availableProcessors();
        int priority;
        String threadName;

        public Builder setThreadNum(int threadNum) {
            this.threadNum = Math.max(1, threadNum);
            return this;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setThreadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public ThreadPoolConfig build() {
            ThreadPoolConfig mThreadPoolConfig = new ThreadPoolConfig();
            mThreadPoolConfig.threadNum = this.threadNum;
            mThreadPoolConfig.priority = this.priority;
            mThreadPoolConfig.threadName = this.threadName;
            return mThreadPoolConfig;
        }
    }


}
