package kuyou.common.ku09.event.rc;

import kuyou.common.ipc.RemoteEvent;
import kuyou.common.ku09.event.rc.basic.EventRemoteControlRequest;

/**
 * action :事件[图片上传请求]
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-3-27 <br/>
 * </p>
 */
public class EventPhotoUploadRequest extends EventRemoteControlRequest {

    public String getImgFilePath() {
        return getData().getString(KEY_IMG_PATH);
    }

    public EventPhotoUploadRequest setImgFilePath(String val) {
        getData().putString(KEY_IMG_PATH, val);
        return EventPhotoUploadRequest.this;
    }

    @Override
    public int getCode() {
        return PHOTO_UPLOAD_REQUEST;
    }

    public static String getImgFilePath(RemoteEvent event) {
        return event.getData().getString(KEY_IMG_PATH);
    }
}