package com.hongfans.retrofitdownload;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hongfans.download.DownloadManager;
import com.hongfans.download.DownloadTask;
import com.hongfans.download.ProgressCallback;
import com.hongfans.download.Util;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "tag_ma";
    private ProgressBar mPb;
    private TextView mProgressTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPb = findViewById(R.id.pb_main);
        mProgressTv = findViewById(R.id.tv_progress);

        DownloadManager.getInstance().init(this);
        DownloadManager.getInstance().addListener(mCallback);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//              String loadUrl = "http://a.gdown.baidu.com/data/wisegame/2828a29ba864e167/neihanduanzi_660.apk?from=a1101";
                String loadUrl = "http://downsz.hongfans.cn:8010/update/apk/Rearview_release_3.2.2.0_20171208.apk";
                String destFileDir = Environment.getExternalStorageDirectory() + "/rearview";
                String destFileName = "app_v3.2.2.0.apk";//文件存放的名称
                String md5 = "A35E55CC90196CCA85342486C50387A8";
                DownloadManager.getInstance().load(loadUrl, destFileDir, destFileName, md5);

            }
        });
        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadManager.getInstance().cancel();
            }
        });
        findViewById(R.id.btn_drop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file = new File(DownloadManager.getInstance().getTask().getPath());
                if (file.exists()) {
                    file.delete();
                    Toast.makeText(MainActivity.this, "成功删除文件", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    int count = 0;

    private ProgressCallback mCallback = new ProgressCallback() {

        @Override
        public void onStart(DownloadTask task) {
            Log.i(TAG, "onStart: " + task);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressTv.setText("onStart");
                }
            });
        }

        @Override
        public void onConnecting(DownloadTask task) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressTv.setText("onConnecting");
                }
            });
            Log.i(TAG, "onConnecting: " + task);
        }

        @Override
        public void onProgress(DownloadTask task) {
            mPb.setMax(100);
            final int progress = (int) (task.getSoFarBytes() * 1.0f / task.getTotalBytes() * 100);
            mPb.setProgress(progress);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressTv.setText("onProgress: " + progress + "%");
                }
            });

            Log.i("more_tag", "onProgress: " + task.getSoFarBytes() + ", " + task.getTotalBytes());

            if ((count % 40) == 0 || task.getSoFarBytes() == task.getTotalBytes()) {
                Log.i(TAG, "onProgress: " + task.getSoFarBytes() + ", " + task.getTotalBytes());
            }
            count++;
        }

        @Override
        public void onCompleted(DownloadTask task) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressTv.setText("onCompleted");
                }
            });

            Log.i(TAG, "onCompleted: " + task.getPath());
            long start = System.currentTimeMillis();
            File file = new File(task.getPath());
            String fileMD5 = Util.getFileMD5(file);
            long l = System.currentTimeMillis() - start;
            // right A35E55CC90196CCA85342486C50387A8
            Log.i(TAG, "onCompleted: " + fileMD5 + ", " + l + ", " + file.length());

            Util.installApkAndStart(MainActivity.this, task.getPath());
        }

        @Override
        public void onError(DownloadTask task, final Throwable e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressTv.setText("onError " + e.getLocalizedMessage());
                }
            });
            Log.e(TAG, "onError: " + e);
        }
    };
}
