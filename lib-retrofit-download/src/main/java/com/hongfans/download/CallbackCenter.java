package com.hongfans.download;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 * Created by MEI on 2017/12/13.
 */

public class CallbackCenter {

    private List<ProgressCallback> mCallbackList = new ArrayList<>();
    private DownloadTask mTask;

    public void setTask(DownloadTask task) {
        mTask = task;
    }

    public DownloadTask getTask(){
        return mTask;
    }

    void addCallback(ProgressCallback callback) {
        mCallbackList.add(callback);
    }

    void removeCallback(ProgressCallback callback) {
        mCallbackList.remove(callback);
    }

    public void progress() {
        for (ProgressCallback callback : mCallbackList) {
            callback.onProgress(mTask);
        }
    }

    public void connecting() {
        for (ProgressCallback callback : mCallbackList) {
            callback.onConnecting(mTask);
        }
    }

    public void start() {
        for (ProgressCallback callback : mCallbackList) {
            callback.onStart(mTask);
        }
    }

    public void complete() {
        for (ProgressCallback callback : mCallbackList) {
            callback.onCompleted(mTask);
        }
    }

    public void error(Throwable e) {
        for (ProgressCallback callback : mCallbackList) {
            callback.onError(mTask, e);
        }
    }
}
