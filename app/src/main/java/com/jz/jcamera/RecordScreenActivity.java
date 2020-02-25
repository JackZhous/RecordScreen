package com.jz.jcamera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import com.jz.jcamera.p.RecorderPresenter;
import com.jz.jcamera.util.Config;
import com.jz.jcamera.util.ToastHelper;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.disposables.Disposable;

public class RecordScreenActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int REQUEST_CODE = 2;
    private Button mBtnRecord;
    private EditText metUrl;
    private MediaProjectionManager mMediaProj;
    private boolean isRecording;
    private String mRtmpUrl;
    private RecorderPresenter presenter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        getPermission();
        initMediaProjection();
    }

    private void initView(){
        metUrl = findViewById(R.id.et_url);
        metUrl.setText(Config.URL);
        mBtnRecord = findViewById(R.id.btn);
        mBtnRecord.setOnClickListener(this);
    }


    private void initMediaProjection(){
        if(Build.VERSION.SDK_INT <= 21){
            ToastHelper.show(this, "sorry! android版本太低，无法使用");
            finish();
            return;
        }
        mMediaProj = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Config.VIDEO_WIDTH = wm.getDefaultDisplay().getWidth();
        Config.VIDEO_HEIGHT = wm.getDefaultDisplay().getHeight();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);

        Config.DENSITY = displayMetrics.densityDpi;
        presenter = new RecorderPresenter(this);
    }

    @Override
    public void onClick(View v) {
        if(isRecording){
            mBtnRecord.setText(R.string.start_record);
            presenter.stop();
            isRecording = false;
        }else {
            mBtnRecord.setText(R.string.stop_record);
            startRecord();
        }
    }

    private void startRecord(){
        if(TextUtils.isEmpty((mRtmpUrl=metUrl.getText().toString().trim()))){
            ToastHelper.show(this, "请输入推流地址!");
            return;
        }

        isRecording = true;
        Intent intent = mMediaProj.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK && requestCode == REQUEST_CODE){
            MediaProjection mp = mMediaProj.getMediaProjection(resultCode, data);
            if(mp == null){
                ToastHelper.show(this, "media proj get error!");
                return;
            }
            presenter.init(mp, mRtmpUrl);
            presenter.start();
        }
    }

    private void getPermission(){
        RxPermissions permissions = new RxPermissions(this);
        Disposable d = permissions.requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE)
                .subscribe(permission -> {
                    if(!permission.granted){
                        ToastHelper.show(RecordScreenActivity.this, "not granted, exit");
                        finish();
                    }
                });
    }
}
