package com.major.demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import java.io.File;


public class MainActivity extends AppCompatActivity{

    private Button BtnDownload;
    private ProgressBar part5PbarProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BtnDownload = (Button)findViewById(R.id.btn_1);
        part5PbarProgress = (ProgressBar)findViewById(R.id.pb_1);

        method();

        resumeDownLoadInfo("200");
    }


    private void method(){
        //获取DownloadManager对象
        final DownloadManager mg = DownloadService.getDownloadManager(this);
        //按钮BtnDownload的点击事件
        BtnDownload.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                try{

                    Data data = new Data();
                    data.id = 200;
                    data.downloadUrl = "http://downsz.hongfans.cn:8010/update/apk/Rearview_release-v2-1496744425.3";
                    data.packageName = "com.hongfans.rearview";
                    data.name = "cyb";

                    //查询指定应用下载记录（断点 progress）
                    DownloadInfo downloadInfo = mg.getDownloadInfoByAppId(data.id + "");
                    //判断是否有下载记录，有就是续传 没有就是开始下载
                    if(downloadInfo == null){
                        //参数1 appId应用编号
                        String appId = data.id + "";
                        //参数2 下载地址
                        String url = data.downloadUrl;
                        //参数3 文件名
                        String fileName = data.name;
                        //参数4 保存的绝对路径
                        String target = Environment.getExternalStorageDirectory().getAbsolutePath() + "/market/" + data.packageName + "v2.apk";
                        //参数5 是否支持断点
                        boolean autoResume = true;
                        //参数6 是否重命名
                        boolean autoRename = false;
                        //参数回调对象，处理界面更新业务逻辑
                        mg.addNewDownload(appId, url, fileName, target, autoResume, autoRename, callback);
                        SL.i("新建下载");
                    } else {
                        //继续传输
                        //载按钮点击事件
                        //loading当前出入下载状态
                        SL.i("state74 " + downloadInfo.getState());

                        if(downloadInfo.getState() == HttpHandler.State.LOADING
                           || downloadInfo.getState() == HttpHandler.State.WAITING){
                            mg.stopDownload(downloadInfo);
                            BtnDownload.setText("继续");
                            SL.i("暂停");
                        } else if(downloadInfo.getState() == HttpHandler.State.SUCCESS){
                            SL.i("下载完成");
                        } else {
                            mg.resumeDownload(downloadInfo, callback);
                            SL.i("恢复下载");
                        }
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void resumeDownLoadInfo(String appId){
        final DownloadManager mg = DownloadService.getDownloadManager(this);
        //5.1获取断点记录
        DownloadInfo downloadInfo = mg.getDownloadInfoByAppId(appId);
        if(downloadInfo == null){
            BtnDownload.setText("下载");
            part5PbarProgress.setMax(100);
            part5PbarProgress.setProgress(0);
        } else {
            //5.2计算百分比
            long total = downloadInfo.getFileLength();
            if(total == 0){
                total = 1;
            }
            int percent = (int)(downloadInfo.getProgress() * 100f / total + 0.5f);
            part5PbarProgress.setMax(100);
            part5PbarProgress.setProgress(percent);
            SL.i("state110 " + downloadInfo.getState());
            if(downloadInfo.getState() == HttpHandler.State.LOADING){
                BtnDownload.setText(percent + "%");
            } else if(downloadInfo.getState() == HttpHandler.State.SUCCESS){
                BtnDownload.setText("安装");
            } else {
                BtnDownload.setText("继续");
            }
        }
    }

    class Data{

        int id;
        String downloadUrl;
        String packageName;
        String name;
    }

    private RequestCallBack<File> callback = new RequestCallBack<File>(){
        //下载过程中进度变化
        @Override
        public void onLoading(long total, long current, boolean isUploading){
            super.onLoading(total, current, isUploading);
            //设置进度条的进度文本
            //设置进度条最大值、设置当前进度
            if(total == 0){
                total = 1;
            }
            int percent = (int)(current * 100f / total + 0.5f);
            BtnDownload.setText(percent + "%");
            part5PbarProgress.setMax(100);//PbarProgress进度条
            part5PbarProgress.setProgress(percent);
            SL.i("下载进度 " + percent + " " + current + "/" + total);
        }

        @Override
        public void onSuccess(ResponseInfo<File> responseInfo){
            BtnDownload.setText("安装");
            part5PbarProgress.setMax(100);
            part5PbarProgress.setProgress(100);
            SL.i("下载完成 " + responseInfo);
            //调用隐式意图安装
//            ApkUtil.install(MainActivity.this, responseInfo.result.getAbsolutePath());
        }

        @Override
        public void onFailure(HttpException e, String s){
            SL.i("下载失败 " + s + ", " + e);

            BtnDownload.setText("继续");
        }
    };
}
