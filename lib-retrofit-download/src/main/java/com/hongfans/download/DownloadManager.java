package com.hongfans.download;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * 文件下载管理
 */

public class DownloadManager {

    private static final String TAG = "tag_dm";

    private static DownloadManager instance;

    private Retrofit retrofit;
    private CallbackCenter mCenter;
    private Subscription mSubscribe;
    //    private DownloadTask mTask;
    private SharedPreferences mPreferences;
    private ApiService mApiService;

    public void init(Context ctx) {
        mPreferences = ctx.getApplicationContext().getSharedPreferences("sp_download", Context.MODE_PRIVATE);
    }

    public static DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    private DownloadManager() {

        mCenter = new CallbackCenter();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new ProgressInterceptor(mCenter)) // 拦截进度
                .connectTimeout(20, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl("http://example.com") // 随便传一个合法的 url 即可
                .build();
        mApiService = retrofit.create(ApiService.class);
    }

    public void addListener(ProgressCallback callBack) {
        mCenter.addCallback(callBack);
    }

    public void load(String url, String destFileDir, String filename, String md5) {
        if (mSubscribe != null && !mSubscribe.isUnsubscribed()) {
            Log.w(TAG, "load: 下载任务正在运行");
            return;
        }

        File file = new File(destFileDir, filename);
        if (file.isDirectory()) {
            file.delete();
        }
        if (!file.exists()) {
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
        }

        DownloadTask task = getExistTask(url, file.getAbsolutePath());
        if (task == null) {
            task = new DownloadTask();
            task.setUrl(url);
            task.setFilename(filename);
            task.setPath(file.getAbsolutePath());
            task.setMD5(md5);
            mCenter.setTask(task);
        } else {
            mCenter.setTask(task);
            Log.i(TAG, "load: 下载任务已存在 " + task);
            if (task.getTotalBytes() == task.getSoFarBytes()) {
                // 下载大小一致，已下载
                if (task.getTotalBytes() == file.length()) {
                    // 文件存在
                    Log.i(TAG, "load: 下载任务文件完整");
                    mCenter.complete();
                    return;
                } else {
                    // 记录下载完成，但文件大小与记录不符合
                    file.delete();
                    task.setSoFarBytes(0L);
                }
            } else if(task.getTotalBytes() < task.getSoFarBytes()){
                Log.i(TAG, "下载记录出错");
            } else {
                Log.i(TAG, "load: 继续下载 " + task.getSoFarBytes() + "/" + task.getTotalBytes());

            }
        }

        mSubscribe = mApiService.download(url)
                .subscribeOn(Schedulers.io())//请求网络 在调度者的io线程
                .unsubscribeOn(Schedulers.io())
                .observeOn(Schedulers.io()) //指定线程保存文件
                .doOnNext(new Action1<ResponseBody>() {
                    @Override
                    public void call(ResponseBody body) {
                        saveFile(body, mCenter.getTask());
                        saveSP(mCenter.getTask());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()) //在主线程中更新ui
                .subscribe(new DownloadSubscriber<ResponseBody>(mCenter));
    }

    private DownloadTask getExistTask(String url, String path) {
        if (mPreferences == null) {
            throw new RuntimeException("invoke init first");
        }
        File file = new File(path);
        String json = mPreferences.getString(url, null);
        if (json != null) {
            try {
                DownloadTask task = new DownloadTask().fromJson(json);
                if (task != null) {
                    if (task.getTotalBytes() == 0L) {
                        throw new RuntimeException("the TotalBytes in sp is 0");
                    }
                    if (task.getSoFarBytes() != file.length()) {
                        // 文件已下载大小与记录大小不一致，更新记录大小为文件大小已下载大小
                        task.setSoFarBytes(file.length());
                        saveSP(task);
                        Log.w(TAG, "getExistTask: 文件大小与记录大小不一致，更新记录大小为文件大小已下载大小");
                    } else {
                        Log.i(TAG, "getExistTask: 文件大小与记录大小一致");
                    }
                }
                return task;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            // 记录不存在，但文件存在，删除对应文件
            // TODO: 2017/12/14 可以根据 MD5 修正记录
            if (file.exists()) {
                file.delete();
            }
        }

        return null;
    }

    public DownloadTask getTask(){
        return mCenter.getTask();
    }

    public void cancel() {
        if (mSubscribe != null && !mSubscribe.isUnsubscribed()) {
            mSubscribe.unsubscribe();
            Log.w(TAG, "load: 取消任务");
        }
    }

    public void addCallback(ProgressCallback callback) {
        mCenter.addCallback(callback);
    }

    public void removeCallback(ProgressCallback callback) {
        mCenter.removeCallback(callback);
    }

    private void saveFile(ResponseBody body, DownloadTask task) {
        if (body == null || task == null) {
            Log.i(TAG, "saveFile: param(s) is null " + body + ", " + task);
            return;
        }
        InputStream is = null;
        byte[] buf = new byte[2048];
        int len;
        FileOutputStream fos = null;
        try {
            is = body.byteStream();
            Log.w(TAG, "saveFile: isAppend " + task.isAppend());
            File file = new File(task.getPath());
            fos = new FileOutputStream(file, task.isAppend());

            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Util.close(is, fos);
        }
    }

    // 保存记录
    private void saveSP(DownloadTask task) {
        // 保存记录到 sp
        if (mPreferences == null) {
            throw new RuntimeException("invoke init first");
        }
        try {
            mPreferences.edit().putString(task.getUrl(), task.toJson()).commit();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "save task to sp failed");
        }
    }

    private interface ApiService {

        @Streaming
        @GET
        Observable<ResponseBody> download(@Url String url);
    }
}
