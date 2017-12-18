package com.hongfans.download;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * TODO
 * Created by MEI on 2017/12/14.
 */

public class Util {

    /**
     * 获取单个文件的MD5值
     */
    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int len;
            byte buffer[] = new byte[1024];
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            BigInteger bigInt = new BigInteger(1, digest.digest());
            return bigInt.toString(16).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(in);
        }
    }

    public static void installApkAndStart(Context context, String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    public static void close(Closeable... args) {
        for (Closeable arg : args) {
            if (arg != null) {
                try {
                    arg.close();
                } catch (IOException e) {
//                    e.printStackTrace();
                }
            }
        }
    }
}
