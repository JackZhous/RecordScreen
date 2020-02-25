package com.jz.jcamera.media;

import android.content.Context;

/**
 * usage:
 * create by jz on 20-2-24
 * email: 651410315@qq.com
 */
public interface Push {
    public void stop() ;

    public  void initPush(final String serverIP, final String serverPort, final String streamName, final Context context, final InitCallback callback);
    public  void initPush(final String url, final Context context, final InitCallback callback, int pts);
    public  void initPush(final String url, final Context context, final InitCallback callback);

    public  void push(byte[] data, int offset, int length, long timestamp, int type);
    public  void push(byte[] data, long timestamp, int type);
}
