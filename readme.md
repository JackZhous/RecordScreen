# android端录屏demo


## 简介

使用EasyRtmp推流库进行推流，本地使用MediaProjectionManager（android 5.0+）进行录屏，AudioRecord音频录制；
android硬解码，从coder取出h264编码好的数据，配合EasyRtmp推流

## 结果

本地使用nginx+ffmpeg进行演示，能正常进行拉流

![演示](test.png)
