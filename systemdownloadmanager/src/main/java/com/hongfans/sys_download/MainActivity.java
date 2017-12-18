package com.hongfans.sys_download;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.hongfans.sys_download.download.Updater;
import com.hongfans.sys_download.download.UpdaterConfig;

public class MainActivity extends AppCompatActivity {

    String APK_URL = "http://downsz.hongfans.cn:8010/update/apk/Rearview_release_3.2.2.0_20171208.apk";

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = findViewById(R.id.et_download);
        editText.setText(APK_URL);
        //如果没有停用,先去停用,然后点击下载按钮. 测试用户关闭下载服务
        //UpdaterUtils.showDownloadSetting(this);
    }

    public void download(View view){
        String url = editText.getText().toString();
        if(TextUtils.isEmpty(editText.getText().toString())){
            url = APK_URL;
        }
        UpdaterConfig config = new UpdaterConfig.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setDescription("版本更新")
                .setFileUrl(url)
                .setCanMediaScanner(true)
                .build();
        Updater.get().showLog(true).download(config);
    }


}
