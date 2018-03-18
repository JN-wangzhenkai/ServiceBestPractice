package com.example.servicebestpractice;

/**
 * Created by wangzhenkai on 2018/3/18.
 */

public interface DownloadListener {


    void onprogress(int progress);
    void onSucess();
    void onFailed();
    void onPause();
    void onCanceled();

}
