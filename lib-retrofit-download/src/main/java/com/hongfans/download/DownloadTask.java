package com.hongfans.download;

import org.json.JSONException;
import org.json.JSONObject;

public class DownloadTask {

    // ----保存信息-----------------------------------
    private String mUrl;
    private String mPath;
    private String mFilename;
    private long mTotalBytes; //  文件总大小
    private long mSoFarBytes; //已加载文件的大小
    private String mMD5; // 用于校验包的完整性，有则校验
    // ----------------------------------------------

    private int mAutoRetryTimes = 0;
    private Object mTag; // 多任务下载时的一个标记
    private boolean isAppend;

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public String getFilename() {
        return mFilename;
    }

    public void setFilename(String filename) {
        mFilename = filename;
    }

    public long getTotalBytes() {
        return mTotalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        mTotalBytes = totalBytes;
    }

    public long getSoFarBytes() {
        return mSoFarBytes;
    }

    public void setSoFarBytes(long soFarBytes) {
        mSoFarBytes = soFarBytes;
    }

    public int getAutoRetryTimes() {
        return mAutoRetryTimes;
    }

    public void setAutoRetryTimes(int autoRetryTimes) {
        mAutoRetryTimes = autoRetryTimes;
    }

    public Object getTag() {
        return mTag;
    }

    public void setTag(Object tag) {
        mTag = tag;
    }

    public String getMD5() {
        return mMD5;
    }

    public void setMD5(String MD5) {
        mMD5 = MD5;
    }

    public boolean isAppend() {
        return isAppend;
    }

    public void setAppend(boolean append) {
        isAppend = append;
    }

    public String toJson() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.putOpt("mUrl", mUrl);
        jo.putOpt("mPath", mPath);
        jo.putOpt("mFilename", mFilename);
        jo.putOpt("mTotalBytes", mTotalBytes);
        jo.putOpt("mSoFarBytes", mSoFarBytes);
        jo.putOpt("mMD5", mMD5);
        return jo.toString();
    }

    public DownloadTask fromJson(String json) throws JSONException {
        JSONObject jo = new JSONObject(json);
        mUrl = jo.optString("mUrl");
        mPath = jo.optString("mPath");
        mFilename = jo.optString("mFilename");
        mTotalBytes = jo.optLong("mTotalBytes");
        mSoFarBytes = jo.optLong("mSoFarBytes");
        mMD5 = jo.optString("mMD5");
        return this;
    }

    @Override
    public String toString() {
        return "DownloadTask{" +
                "mUrl='" + mUrl + '\'' +
                ", mPath='" + mPath + '\'' +
                ", mFilename='" + mFilename + '\'' +
                ", mTotalBytes=" + mTotalBytes +
                ", mSoFarBytes=" + mSoFarBytes +
                ", mAutoRetryTimes=" + mAutoRetryTimes +
                ", mTag=" + mTag +
                ", mMD5='" + mMD5 + '\'' +
                '}';
    }
}
