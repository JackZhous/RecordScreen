package com.jz.jcamera.media;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.view.Surface;

import com.jz.jcamera.util.LogHelper;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * usage: 屏幕录制管理类： 屏幕录制以及编码
 * create by jz on 20-2-24
 * email: 651410315@qq.com
 */
public class ScreenManager extends Thread{
    private int mWidth,mHeight;
    private MediaProjection mMediaProj;
    private int mBitrate;       //输出码率
    private int mDpi;

    private MediaCodec mCodec;
    private VirtualDisplay mVirDis;
    private Surface mSurface;
    private boolean mQuit;
    private byte[] mPpsSps;
    private byte[] h264;
    private Push pusher;

    /**
     * encoder param
     */
    private static final int TIMEOUT_US = 10000;
    private MediaCodec.BufferInfo mBufferVideo = new MediaCodec.BufferInfo();

    private ScreenManager(int w, int h, int dpi, MediaProjection proj, int bitrate, Push rtmp) {
        this.mBitrate = bitrate;
        this.mHeight = h;
        this.mWidth = w;
        mMediaProj = proj;
        mDpi = dpi;
        pusher = rtmp;
    }


    public void stopScreen(){
        mQuit = true;
    }

    private void configureMedia() throws IOException {
        //类型找编码器，通过名字找到的服务端不一定能识别这个编码器，我用ffmpeg有时解码找不到编码器
        mCodec = MediaCodec.createEncoderByType("video/avc");

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 20000000);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

        mCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        // 获取Surface对象
        mSurface = mCodec.createInputSurface();

        mCodec.start();
    }


//
    @Override
    public void run() {
        try {
            configureMedia();
            h264 = new byte[mWidth * mHeight];
            //创建虚拟屏幕，用于录制当前的手机视频
            mVirDis = mMediaProj.createVirtualDisplay("screen-display",
                    mWidth, mHeight, mDpi,
                     DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC ,
                    //最后两个参数用于回调display状态以及回调在哪个线程Handler
                    mSurface, null, null);
            LogHelper.de_i("start record display");
            recordDisplay();
        } catch (IOException e) {
            LogHelper.de_i("init video encoder error!");
        }finally {
            release();
        }
    }

    private void recordDisplay(){
        while (!mQuit){
            //阻塞从编码器输出缓存区区数据,返回缓冲区index
            int eobIndex = mCodec.dequeueOutputBuffer(mBufferVideo, TIMEOUT_US);
            if (eobIndex >= 0) {
                packageData(eobIndex);
                mCodec.releaseOutputBuffer(eobIndex, false);
            }
        }
    }


    /**
     */
    private void packageData(int outBuffIndex) {
        ByteBuffer buffer;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buffer = mCodec.getOutputBuffer(outBuffIndex);
        } else {
            buffer = mCodec.getOutputBuffers()[outBuffIndex];
        }

        buffer.position(mBufferVideo.offset);
        buffer.limit(mBufferVideo.offset + mBufferVideo.size);

        try {
            boolean sync = false;

            if ((mBufferVideo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {// sps
                sync = (mBufferVideo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0;

                if (!sync) {
                    byte[] temp = new byte[mBufferVideo.size];
                    buffer.get(temp);
                    mPpsSps = temp;
                    return;
                } else {
                    mPpsSps = new byte[0];
                }
            }

            sync |= (mBufferVideo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0;
            int len = mPpsSps.length + mBufferVideo.size;

            if (len > h264.length) {
                h264 = new byte[len];
            }

            if (sync) {
                System.arraycopy(mPpsSps, 0, h264, 0, mPpsSps.length);
                buffer.get(h264, mPpsSps.length, mBufferVideo.size);
                pusher.push(h264, 0, mPpsSps.length + mBufferVideo.size, mBufferVideo.presentationTimeUs / 1000, 2);
            } else {
                buffer.get(h264, 0, mBufferVideo.size);
                pusher.push(h264, 0, mBufferVideo.size, mBufferVideo.presentationTimeUs / 1000, 1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    private void release(){
        mCodec.release();
        mCodec = null;
    }

    public static class Builder{
        private int width;
        private int height;
        private int density;
        private MediaProjection mp;
        private int vibrate;
        private Push pusher;

        public Builder setVibrate(int vibrate) {
            this.vibrate = vibrate;
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setDensity(int density) {
            this.density = density;
            return this;
        }

        public Builder setMp(MediaProjection mp) {
            this.mp = mp;
            return this;
        }

        public Builder setPusher(Push pusher) {
            this.pusher = pusher;
            return this;
        }

        public ScreenManager build(){
            return new ScreenManager(width, height, density, mp, vibrate, pusher);
        }
    }
}
