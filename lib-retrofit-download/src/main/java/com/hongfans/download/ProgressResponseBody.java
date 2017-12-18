package com.hongfans.download;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class ProgressResponseBody extends ResponseBody {

    private CallbackCenter mCenter;
    private BufferedSource bufferedSource;
    private ResponseBody mResponseBody;

    public ProgressResponseBody(ResponseBody responseBody, CallbackCenter center) {
        mResponseBody = responseBody;
        mCenter = center;
    }

    @Override
    public MediaType contentType() {
        return mResponseBody.contentType();
    }

    @Override
    public long contentLength() {
        return mResponseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(mResponseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        Log.w("tag_source", "source: init");
        final DownloadTask task = mCenter.getTask();
        if (task == null) {
            throw new RuntimeException("task is null");
        }

        return new ForwardingSource(source) {
            long bytesReaded = task.getSoFarBytes();

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);

                if (bytesRead == -1) {
                    // 读写文件完成，校验 MD5
                    Log.w("tag_source", "read: 读写文件完成，校验 MD5 " + Util.getFileMD5(new File(task.getPath())));
                } else {
                    bytesReaded += bytesRead;
                }

//                bytesReaded += bytesRead == -1 ? 0 : bytesRead;

                task.setSoFarBytes(bytesReaded);

                // 发送进度
                mCenter.progress();

                return bytesRead;
            }
        };
    }
}
