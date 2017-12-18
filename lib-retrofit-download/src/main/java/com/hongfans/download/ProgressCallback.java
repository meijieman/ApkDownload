package com.hongfans.download;

public interface ProgressCallback {

    /**
     * 下载前校验，是否已下载，是否断点下载，文件是否完整
     */
    void onStart(DownloadTask task);

    /**
     * 开始请求网络下载
     */
    void onConnecting(DownloadTask task);
    /**
     * 网络下载进度
     */
    void onProgress(DownloadTask task);

    /**
     * 下载完成
     */
    void onCompleted(DownloadTask task);

    /**
     * 下载出错
     */
    void onError(DownloadTask task, Throwable e);
}