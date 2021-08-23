package com.kuyou.avc.handler;

import android.os.Bundle;
import android.util.Log;

import com.kuyou.avc.handler.basic.IAudioVideoRequestCallback;
import com.kuyou.avc.handler.photo.ITakePhotoResultListener;
import com.kuyou.avc.handler.photo.TakePhotoBackground;
import com.kuyou.avc.ui.basic.AVCActivity;

import kuyou.common.ipc.RemoteEvent;
import kuyou.common.ku09.event.avc.EventPhotoTakeRequest;
import kuyou.common.ku09.event.avc.EventPhotoTakeResult;
import kuyou.common.ku09.event.avc.basic.EventAudioVideoCommunication;
import kuyou.common.ku09.event.rc.EventPhotoUploadRequest;
import kuyou.common.ku09.handler.BasicEventHandler;
import kuyou.common.ku09.protocol.IJT808ExtensionProtocol;

public class PhotoTakeHandler extends BasicEventHandler implements ITakePhotoResultListener {

    protected final String TAG = "com.kuyou.avc.handle > PhotoTakeHandler";

    public interface Policy {
        /**
         * action :未定位时使用缓存位置
         */
        public static final int FOREGROUND_TAKE = (1 << 0);
        /**
         * action :使用原生定位位置 <br/>
         * remark :<br/>
         * 01 互斥策略：PROVIDER_AMAP
         */
        public static final int BACKGROUND_TAKE = (1 << 1);
        /**
         * action :使用原生定位位置 <br/>
         * remark :<br/>
         * 01 互斥策略：PROVIDER_AMAP
         */
        public static final int SCREENSHOT = (1 << 2);
    }

    private IAudioVideoRequestCallback mAudioVideoRequestCallback;

    public IAudioVideoRequestCallback getAudioVideoRequestCallback() {
        return mAudioVideoRequestCallback;
    }

    public PhotoTakeHandler setAudioVideoRequestCallback(IAudioVideoRequestCallback callback) {
        this.mAudioVideoRequestCallback = callback;
        return PhotoTakeHandler.this;
    }

    public boolean isItInHandlerState(int handlerStatus) {
        if (null == getAudioVideoRequestCallback()) {
            Log.e(TAG, "isItInHandlerState > process fail : AudioVideoRequestCallback is null");
            return false;
        }
        return handlerStatus == getAudioVideoRequestCallback().getHandlerStatus();
    }

    public boolean isLiveOnlineByType(int typeCode) {
        if (null == getAudioVideoRequestCallback()) {
            Log.e(TAG, "isLiveOnlineByType > process fail : AudioVideoRequestCallback is null");
            return false;
        }
        return getAudioVideoRequestCallback().getOnlineList().containsKey(typeCode);
    }

    @Override
    public boolean onModuleEvent(RemoteEvent event) {
        switch (event.getCode()) {

            case EventAudioVideoCommunication.Code.PHOTO_TAKE_REQUEST:
                Log.d(TAG, "onModuleEvent > 处理拍照请求");

                if (IJT808ExtensionProtocol.EVENT_TYPE_LOCAL_DEVICE_INITIATE == EventPhotoTakeRequest.getEventType(event)) {
                    play("正在为您拍照");
                }

                //截图拍照
                if (isItInHandlerState(IAudioVideoRequestCallback.HS_OPEN)) {
                    int onLineTypeCode = -1;
                    if (isLiveOnlineByType(IJT808ExtensionProtocol.MEDIA_TYPE_VIDEO)) {
                        onLineTypeCode = IJT808ExtensionProtocol.MEDIA_TYPE_VIDEO;
                    }
                    if (isLiveOnlineByType(IJT808ExtensionProtocol.MEDIA_TYPE_THERMAL)) {
                        onLineTypeCode = IJT808ExtensionProtocol.MEDIA_TYPE_THERMAL;
                    }
                    if (-1 != onLineTypeCode) {
                        int result = getAudioVideoRequestCallback().getOnlineList()
                                .get(onLineTypeCode)
                                .screenshot(event, new AVCActivity.IVideoCameraResultListener() {
                                    @Override
                                    public void onScreenshotResult(String result) {
                                        PhotoTakeHandler.this.onTakePhotoResult(true, result, event.getData());
                                    }
                                });
                        if (-1 != result) {//异常失败处理
                            PhotoTakeHandler.this.onTakePhotoResult(false, "", event.getData());
                        }
                        return true;
                    }
                }
                //后台相机拍照
                TakePhotoBackground.perform(getContext(), event.getData(), PhotoTakeHandler.this);
                ////前台相机拍照
                //TakePhoto.perform(getContext(), event.getData(), PhotoTakeHandler.this);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onTakePhotoResult(boolean result, String info, Bundle data) {
        if (result) {
            Log.d(TAG, "onResult > 拍照成功 > 申请上传");
            if (IJT808ExtensionProtocol.EVENT_TYPE_LOCAL_DEVICE_INITIATE == EventPhotoTakeResult.getEventType(data)) {
                play("拍照成功");
            }
            dispatchEvent(new EventPhotoUploadRequest()
                    .setImgFilePath(info)
                    .setEventType(EventPhotoUploadRequest.getEventType(data))
                    .setRemote(true));
        } else {
            Log.d(TAG, "onResult > 拍照失败");
            if (IJT808ExtensionProtocol.EVENT_TYPE_LOCAL_DEVICE_INITIATE == EventPhotoTakeResult.getEventType(data)) {
                play("拍照失败");
            }
            dispatchEvent(new EventPhotoTakeResult()
                    .setData(data)
                    .setRemote(true)
                    .setResult(false));
        }
    }
}