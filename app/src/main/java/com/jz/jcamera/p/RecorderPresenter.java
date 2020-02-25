package com.jz.jcamera.p;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.media.projection.MediaProjection;

import com.jz.jcamera.media.AudioManager;
import com.jz.jcamera.media.Push;
import com.jz.jcamera.media.ScreenManager;
import com.jz.jcamera.util.Config;
import com.jz.jcamera.util.LogHelper;
import com.jz.jcamera.util.ToastHelper;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.easydarwin.easyrtmp.push.EasyRTMP;

/**
 * usage:
 * create by jz on 20-2-25
 * email: 651410315@qq.com
 */
public class RecorderPresenter {
    private ScreenManager mScreenManger;
    private AudioManager mAudioManger;
    private Push mRtmpClient;

    public Activity mCtx;

    public RecorderPresenter(Activity mCtx) {
        this.mCtx = mCtx;
    }

    public void init(MediaProjection mp, String url){
        mRtmpClient = new EasyRTMP(EasyRTMP.VIDEO_CODEC_H264, Config.RTMP_KEY);
        mRtmpClient.initPush(url, mCtx, code -> LogHelper.de_i(code+""));
        mScreenManger = new ScreenManager.Builder()
                                .setDensity(Config.DENSITY)
                                .setHeight(Config.VIDEO_HEIGHT)
                                .setWidth(Config.VIDEO_WIDTH)
                                .setMp(mp)
                                .setPusher(mRtmpClient)
                                .setVibrate(Config.VIDEO_BITRATE)
                                .build();
        mAudioManger = new AudioManager(mRtmpClient);
    }


    public void start(){
        if(mScreenManger == null || mAudioManger == null){
            ToastHelper.show(mCtx, "presenter is not init");
            return;
        }

        mScreenManger.start();
        mAudioManger.startRecord();
    }


    public void stop(){
        mScreenManger.stopScreen();
        mAudioManger.stop();
        mRtmpClient.stop();
    }




}
