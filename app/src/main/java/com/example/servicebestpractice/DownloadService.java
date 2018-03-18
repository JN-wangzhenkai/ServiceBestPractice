package com.example.servicebestpractice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private DownloadTask downloadTask;
    private String downloadUrl;


    private NotificationManager getNotificationmanager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);

        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentIntent(pi);
        builder.setContentTitle(title);

        if (progress > 0) {
            //显示下载进度
            builder.setContentText(progress + "%");
            builder.setProgress(1000, progress, false);

        }
        return builder.build();


    }

    private DownloadListener listener = new DownloadListener() {

        @Override
        public void onprogress(int progress) {
            getNotificationmanager().notify(1, getNotification("downloading...", progress));

        }

        @Override
        public void onSucess() {

            downloadTask = null;
            //下载成功后将前台服务 通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            getNotificationmanager().notify(1, getNotification("下载成功", -1));
            Toast.makeText(DownloadService.this, "download success", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onFailed() {
            downloadTask = null;

//失败后 通知服务关闭，并创建一个下载失败的通知
            stopForeground(true);
            getNotificationmanager().notify(1, getNotification("download  failed", -1));

        }

        @Override
        public void onPause() {
            downloadTask = null;
            Toast.makeText(DownloadService.this, "Paused", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onCanceled() {

            downloadTask=null;
            stopForeground(true);
        }
    };

    public DownloadService() {


    }


    class DownloadBinder extends Binder{
        public void startDownLoad(String url){
            if(downloadTask==null){
                downloadUrl=url;
                downloadTask=new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                startForeground(1,getNotification("doenloading...",0));
            }
        }

        public void pauseDownLoad(){
            if(downloadTask!=null){
                downloadTask.pauseDownload();
            }
        }

        public void cancle(){
            if(downloadTask!=null){
                downloadTask.canceled();
            }else {
                if(downloadUrl!=null){
                    //取消下载时需要将文件删除，并将通知关闭
                     String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                     String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

                    File file=new File(directory+fileName);

                    if(file.exists()){file.delete();}

                    getNotificationmanager().cancel(1);
                    stopForeground(true);
                }
            }
        }
    }


    private DownloadBinder mbinder=new DownloadBinder();
    @Override
    public IBinder onBind(Intent intent) {
      return  mbinder;
    }
}
