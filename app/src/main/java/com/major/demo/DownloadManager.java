package com.major.demo;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.db.converter.ColumnConverter;
import com.lidroid.xutils.db.converter.ColumnConverterFactory;
import com.lidroid.xutils.db.sqlite.ColumnDbType;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @desc: TODO
 * @author: Major
 * @since: 2017/8/14 1:12
 * <p>
 * 断点下载续传—支持断点续传 ,后台有服务来下载文件。
 * http://blog.csdn.net/qq_37293612/article/details/54562451
 */
public class DownloadManager{

    private List<DownloadInfo> downloadInfoList;

    private int maxDownloadThread = 3;

    private Context mContext;
    private DbUtils db;

    DownloadManager(Context appContext){
        ColumnConverterFactory.registerColumnConverter(HttpHandler.State.class, new HttpHandlerStateConverter());
        mContext = appContext;
        this.db = DbUtils.create(mContext);// xUtils.db
        try{
            downloadInfoList = this.db.findAll(Selector.from(DownloadInfo.class));
        } catch(DbException e){
            SL.i(e.getMessage());
        }
        if(downloadInfoList == null){
            downloadInfoList = new ArrayList<>();
        }
    }

    public int getDownloadInfoListCount(){
        return downloadInfoList.size();
    }

    public DownloadInfo getDownloadInfo(int index){
        return downloadInfoList.get(index);
    }

    /***
     * 方法 按照应用appId查找断点记录
     *
     * @param appId
     * @return
     */
    public DownloadInfo getDownloadInfoByAppId(String appId){
        for(DownloadInfo item : downloadInfoList){
            if(TextUtils.equals(item.getAppId(), appId)){
                return item;// 返回指定appId的断点记录
            }
        }
        return null;
    }

    public void addNewDownload(String appId, String url, String fileName, String target, boolean autoResume, boolean autoRename,
            final RequestCallBack<File> callback) throws DbException{
        final DownloadInfo downloadInfo = new DownloadInfo();
        downloadInfo.setAppId(appId);
        downloadInfo.setDownloadUrl(url);
        downloadInfo.setAutoRename(autoRename);
        downloadInfo.setAutoResume(autoResume);
        downloadInfo.setFileName(fileName);
        downloadInfo.setFileSavePath(target);
        HttpUtils http = new HttpUtils();
        http.configRequestThreadPoolSize(maxDownloadThread);
        HttpHandler<File> handler = http.download(url, target, autoResume, autoRename, new ManagerCallBack(downloadInfo, callback));
        downloadInfo.setHandler(handler);
        downloadInfo.setState(handler.getState());
        downloadInfoList.add(downloadInfo);
        db.saveBindingId(downloadInfo);
    }

    public void resumeDownload(int index, final RequestCallBack<File> callback) throws DbException{
        final DownloadInfo downloadInfo = downloadInfoList.get(index);
        resumeDownload(downloadInfo, callback);
    }

    public void resumeDownload(DownloadInfo downloadInfo, final RequestCallBack<File> callback) throws DbException{
        HttpUtils http = new HttpUtils();
        http.configRequestThreadPoolSize(maxDownloadThread);
        HttpHandler<File> handler = http.download(downloadInfo.getDownloadUrl(), downloadInfo.getFileSavePath(),
                downloadInfo.isAutoResume(), downloadInfo.isAutoRename(), new ManagerCallBack(downloadInfo, callback));
        downloadInfo.setHandler(handler);
        downloadInfo.setState(handler.getState());
        db.saveOrUpdate(downloadInfo);
    }

    public void removeDownload(int index) throws DbException{
        DownloadInfo downloadInfo = downloadInfoList.get(index);
        removeDownload(downloadInfo);
    }

    public void removeDownload(DownloadInfo downloadInfo) throws DbException{
        HttpHandler<File> handler = downloadInfo.getHandler();
        if(handler != null && !handler.isCancelled()){
            handler.cancel();
        }
        // 从下载的断点记录集合中移除断点记录(内存)
        downloadInfoList.remove(downloadInfo);
        // 从数据库删除。
        db.delete(downloadInfo);
        // 下载一半的文件?100M 55M
        String fileSavePath = downloadInfo.getFileSavePath();
        // 如果存在下载一半的文件，由于下次找不到断点记录，没法续传,直接删除这个文件节省sd空间.
        File file = new File(fileSavePath);
        if(file.exists()){
            file.delete();
        }
    }

    public void stopDownload(int index) throws DbException{
        DownloadInfo downloadInfo = downloadInfoList.get(index);
        stopDownload(downloadInfo);
    }

    public void stopDownload(DownloadInfo downloadInfo) throws DbException{
        HttpHandler<File> handler = downloadInfo.getHandler();
        if(handler != null && !handler.isCancelled()){
            handler.cancel();
        } else {
            downloadInfo.setState(HttpHandler.State.CANCELLED);
        }
        db.saveOrUpdate(downloadInfo);
    }

    public void stopAllDownload() throws DbException{
        for(DownloadInfo downloadInfo : downloadInfoList){
            HttpHandler<File> handler = downloadInfo.getHandler();
            if(handler != null && !handler.isCancelled()){
                handler.cancel();
            } else {
                downloadInfo.setState(HttpHandler.State.CANCELLED);
            }
        }
        db.saveOrUpdateAll(downloadInfoList);
    }

    public void backupDownloadInfoList() throws DbException{
        for(DownloadInfo downloadInfo : downloadInfoList){
            HttpHandler<File> handler = downloadInfo.getHandler();
            if(handler != null){
                downloadInfo.setState(handler.getState());
            }
        }
        db.saveOrUpdateAll(downloadInfoList);
    }

    public int getMaxDownloadThread(){
        return maxDownloadThread;
    }

    public void setMaxDownloadThread(int maxDownloadThread){
        this.maxDownloadThread = maxDownloadThread;
    }

    public class ManagerCallBack extends RequestCallBack<File>{

        private DownloadInfo downloadInfo;
        private RequestCallBack<File> baseCallBack;

        public RequestCallBack<File> getBaseCallBack(){
            return baseCallBack;
        }

        public void setBaseCallBack(RequestCallBack<File> baseCallBack){
            this.baseCallBack = baseCallBack;
        }

        private ManagerCallBack(DownloadInfo downloadInfo, RequestCallBack<File> baseCallBack){
            this.baseCallBack = baseCallBack;
            this.downloadInfo = downloadInfo;
        }

        @Override
        public Object getUserTag(){
            if(baseCallBack == null){
                return null;
            }
            return baseCallBack.getUserTag();
        }

        @Override
        public void setUserTag(Object userTag){
            if(baseCallBack == null){
                return;
            }
            baseCallBack.setUserTag(userTag);
        }

        @Override
        public void onStart(){
            HttpHandler<File> handler = downloadInfo.getHandler();
            if(handler != null){
                downloadInfo.setState(handler.getState());
            }
            try{
                db.saveOrUpdate(downloadInfo);
            } catch(DbException e){
                SL.i(e.getMessage());
            }
            if(baseCallBack != null){
                baseCallBack.onStart();
            }
        }

        @Override
        public void onCancelled(){
            HttpHandler<File> handler = downloadInfo.getHandler();
            if(handler != null){
                downloadInfo.setState(handler.getState());
            }
            try{
                db.saveOrUpdate(downloadInfo);
            } catch(DbException e){
                SL.i(e.getMessage());
            }
            if(baseCallBack != null){
                baseCallBack.onCancelled();
            }
        }

        @Override
        public void onLoading(long total, long current, boolean isUploading){
            HttpHandler<File> handler = downloadInfo.getHandler();
            if(handler != null){
                downloadInfo.setState(handler.getState());
            }
            downloadInfo.setFileLength(total);
            downloadInfo.setProgress(current);
            try{
                db.saveOrUpdate(downloadInfo);
            } catch(DbException e){
                SL.i(e.getMessage());
            }
            if(baseCallBack != null){
                baseCallBack.onLoading(total, current, isUploading);
            }
        }

        @Override
        public void onSuccess(ResponseInfo<File> responseInfo){
            HttpHandler<File> handler = downloadInfo.getHandler();
            if(handler != null){
                downloadInfo.setState(handler.getState());
            }
            try{
                db.saveOrUpdate(downloadInfo);
            } catch(DbException e){
                SL.i(e.getMessage());
            }
            if(baseCallBack != null){
                baseCallBack.onSuccess(responseInfo);
            }
        }

        @Override
        public void onFailure(HttpException error, String msg){
            HttpHandler<File> handler = downloadInfo.getHandler();
            if(handler != null){
                downloadInfo.setState(handler.getState());
            }
            try{
                db.saveOrUpdate(downloadInfo);
            } catch(DbException e){
                SL.i(e.getMessage());
            }
            if(baseCallBack != null){
                baseCallBack.onFailure(error, msg);
            }
        }
    }

    private class HttpHandlerStateConverter implements ColumnConverter<HttpHandler.State>{

        @Override
        public HttpHandler.State getFieldValue(Cursor cursor, int index){
            return HttpHandler.State.valueOf(cursor.getInt(index));
        }

        @Override
        public HttpHandler.State getFieldValue(String fieldStringValue){
            if(fieldStringValue == null){
                return null;
            }
            return HttpHandler.State.valueOf(fieldStringValue);
        }

        @Override
        public Object fieldValue2ColumnValue(HttpHandler.State fieldValue){
            return fieldValue.value();
        }

        @Override
        public ColumnDbType getColumnDbType(){
            return ColumnDbType.INTEGER;
        }
    }
}
