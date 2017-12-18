package com.hongfans.download;

import rx.Subscriber;

public class DownloadSubscriber<T> extends Subscriber<T> {

    private static final String TAG = "tag_ds";
    private CallbackCenter mCenter;

    public DownloadSubscriber(CallbackCenter center) {
        mCenter = center;
    }

    @Override
    public void onStart() {
        if (mCenter != null) {
            mCenter.start();
        }
    }

    @Override
    public void onCompleted() {
        if (mCenter != null) {
            mCenter.complete();
        }
    }

    @Override
    public void onError(Throwable e) {
        if (mCenter != null) {
            mCenter.error(e);
        }
    }

    @Override
    public void onNext(T t) {
        //　do nothing
    }
}