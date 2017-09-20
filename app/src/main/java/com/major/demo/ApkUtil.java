package com.major.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * @desc: TODO
 * @author: Major
 * @since: 2017/8/14 1:22
 */
public class ApkUtil{

    public static void install(Activity activity,String target){
        //Intent :开启服务或者打开页面
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        String type = "application/vnd.android.package-archive";
        // http://www.itheima.com f  /mnt/sdcard
        Uri data = Uri.parse("file:///" + target);
        intent.setDataAndType(data, type);
        activity.startActivity(intent);
    }
}
