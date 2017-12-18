package com.hongfans.download;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ProgressInterceptor<T> implements Interceptor {

    private static final String TAG = "tag_pi";

    private CallbackCenter mCenter;

    public ProgressInterceptor(CallbackCenter center) {
        mCenter = center;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        //　获取断点
        long startPoints = getStartPoints(mCenter.getTask());
        Log.w(TAG, "intercept: startPoints " + startPoints);

        // 设置回调
        mCenter.connecting();

        Request request = chain.request().newBuilder()
                .addHeader("Range", "bytes=" + startPoints + "-")
                .build();

        Response originalResponse = chain.proceed(request);

        String header = originalResponse.header("Content-Range"); // bytes 0-20694714/20694715
        if (header != null) {
            String substring = header.substring(header.indexOf("/") + 1);
            mCenter.getTask().setTotalBytes(Long.parseLong(substring));
        } else {
            Log.e(TAG, "Content-Range is null");
        }
        return originalResponse.newBuilder()
                .body(new ProgressResponseBody(originalResponse.body(), mCenter))
                .build();
    }

    private long getStartPoints(DownloadTask task) {
        long startPoints = 0; //startpos 已经下载的大小
        if (task != null) {
            File file = new File(task.getPath());
            if (file.exists()) {
                if (file.isFile()) {
                    Log.i(TAG, "totalBytes " + task.getTotalBytes());
                    if (task.getTotalBytes() <= 0) {
                        Log.e(TAG, "totalBytes error");
                    } else {
                        if (task.getTotalBytes() != task.getSoFarBytes()) {
                            task.setAppend(true);
                            startPoints = task.getSoFarBytes();
                            task.setSoFarBytes(task.getSoFarBytes());
                            task.setTotalBytes(task.getTotalBytes());
                        } else {
                            Log.e(TAG, "already downloaded");
                        }
                    }
                } else {
                    file.delete();
                }
            }
        } else {
            throw new RuntimeException("task is null");
        }
        return startPoints;
    }
}
