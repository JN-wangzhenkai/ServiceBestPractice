package com.example.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import org.w3c.dom.ProcessingInstruction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by wangzhenkai on 2018/3/18.
 */

public class DownloadTask extends AsyncTask<String,Integer,Integer> {//string传给后台，整型显示进度，整型反馈
    public static final int TYPE_SUCCESS=0;
    public static final int TYPE_FAILED=1;
    public static final int TYPE_PAUSE=2;
    public static final int TYPE_CANCEL=3;

    private DownloadListener  downloadListener;
    private boolean isCanceled=false;
    private boolean isPaused=false;
    private int lastProgress;

    public DownloadTask(DownloadListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    @Override
    protected Integer doInBackground(String... strings) {

        InputStream in=null;
        File file=null;
        RandomAccessFile savedFile=null;
        try {

        long downloadLength=0;
        String downloadUrl=strings[0];
        String fileName=downloadUrl.substring(downloadUrl.lastIndexOf("/"));//文件名
        //sd 卡download 目录
        String directory= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

         file=new File(directory+fileName);
        //判断是否存在
        if(file.exists()){
            downloadLength=file.length();//获取长度，看下载完成否
        }

            long contentLength=getContetnLength(downloadUrl);
        if(contentLength==0){
            return TYPE_FAILED;}
        else if(contentLength==downloadLength){
            return TYPE_SUCCESS;//下载完
        }

        OkHttpClient okHttpClient=new OkHttpClient();
        Request request=new Request.Builder()
                //断点续传 指定从那个点开始
                .addHeader("RANGE","bytes="+downloadLength+"-")
                .url(downloadUrl)
                .build();
        Response response=okHttpClient.newCall(request).execute();

        if(response!=null){
            in=response.body().byteStream();
            savedFile=new RandomAccessFile(file,"rw");
            savedFile.seek(downloadLength);//跳过已经下载的字节

            byte []b=new byte[1024];
            int total=0;
            int len;
            while ((len=in.read(b))!=-1){
                if(isCanceled){
                    return  TYPE_CANCEL;//取消
                }else if(isPaused){
                    return TYPE_PAUSE;//暂停
                }else {
                    total+=len;
                    savedFile.write(b,0,len);

                    //计算已经下载的百分比
                int progress= (int) ((total+downloadLength)*100/contentLength);
                publishProgress(progress);
                }
            }

            response.body().close();
            return TYPE_SUCCESS;
        }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
                try {
                    if(in!=null){
                    in.close();}
                    if(savedFile!=null){
                        savedFile.close();
                    }
                    if(isCanceled&&file!=null){
                        file.delete();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return TYPE_FAILED;
        }


    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress=values[0];
        if (progress>lastProgress){
            downloadListener.onprogress(progress);
            lastProgress=progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case TYPE_SUCCESS:
                downloadListener.onSucess();
                break;
            case TYPE_PAUSE:
                downloadListener.onPause();
                break;
            case TYPE_CANCEL:
                downloadListener.onCanceled();
                break;
            case TYPE_FAILED:
                downloadListener.onFailed();
                break;
            default:
                break;
        }

    }
        public void  pauseDownload(){isPaused=true;}
    public  void canceled(){isCanceled=true;}

    //获取文件长度
    private long getContetnLength(String downloadUrl)throws IOException{
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder()
                .url(downloadUrl)
                .build();

        Response response=client.newCall(request).execute();
        if(response!=null&&response.isSuccessful()){
            long contentLength=response.body().contentLength();
            response.body().close();

            return contentLength;
        }
        return 0;
    }
}
