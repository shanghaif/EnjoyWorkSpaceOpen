package kuyou.common.ipc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * action :IPC框架服务
 * <p>
 * author: wuguoxian <br/>
 * date: 21-7-22 <br/>
 * remarks:  <br/>
 * 未实现：模块注册状态回调</p>
 * 未实现：事件Code注册回调</p>
 */
public class FrameRemoteService extends Service {
    protected final String TAG = "kuyou.common.ipc > FrameRemoteService";

    private RemoteCallbackList<IRemoteServiceCallBack> mCallbackList = new RemoteCallbackList<IRemoteServiceCallBack>();
    //private Map<String, IRemoteServiceCallBack> mModuleCallbackList = new HashMap<>();
    private List<String> mRegisterModuleList = new ArrayList<>();

    private IRemoteService.Stub mBinder = new IRemoteService.Stub() {

        @Override
        public void sendEvent(Bundle data) throws RemoteException {
            if (data == null) {
                Log.e(TAG, "sendEvent > process fail : data is null");
                return;
            }
            if (null == mCallbackList) {
                mCallbackList = new RemoteCallbackList<IRemoteServiceCallBack>();
                Log.e(TAG, "sendEvent > mCallbackList is null");
                return;
            }
            Log.d(TAG, "registerCallback > sendEvent = " + RemoteEvent.getCodeByData(data));
            int N = beginBroadcastCallback(mCallbackList);
            for (int i = 0; i < N; i++) {
                try {
                    mCallbackList.getBroadcastItem(i).onReceiveEvent(data);
                } catch (Exception e) {
                    Log.e(TAG, new StringBuilder("sendEvent > process fail : \n")
                            .append("event = ").append(RemoteEvent.getCodeByData(data))
                            //.append("\n").append(Log.getStackTraceString(e))
                            .toString());
                }
            }
            mCallbackList.finishBroadcast();
        }

        @Override
        public void registerCallback(String packageName, IRemoteServiceCallBack cb) throws RemoteException {
            if (packageName == null) {
                Log.e(TAG, "registerCallback > process fail : packageName is null");
                return;
            }
            if (cb == null) {
                Log.e(TAG, "registerCallback > process fail : IRemoteServiceCallBack is null");
                return;
            }
            Log.d(TAG, "registerCallback > packageName = " + packageName);
            mCallbackList.register(cb);
            mRegisterModuleList.add(packageName);
        }

        @Override
        public void unregisterCallback(String packageName, IRemoteServiceCallBack cb) throws RemoteException {
            if (packageName == null) {
                Log.e(TAG, "unregisterCallback > process fail : packageName is null");
                return;
            }
            if (cb == null) {
                Log.e(TAG, "unregisterCallback > process fail : IRemoteServiceCallBack is null");
                return;
            }
            Log.d(TAG, "unregisterCallback > packageName = " + packageName);
            mCallbackList.unregister(cb);
            mRegisterModuleList.remove(packageName);
        }

        @Override
        public List<String> getRegisterModules() throws RemoteException {
            //Log.d(TAG, "getRegisterModules >  ");
            return mRegisterModuleList;
        }

    };

    private int beginBroadcastCallback(RemoteCallbackList callback) {
        int N = -1;
        try {
            N = callback.beginBroadcast();
        } catch (Exception e) {
            callback.finishBroadcast();
            N = callback.beginBroadcast();
        }
        if (N == 0) {
            callback.finishBroadcast();
            return -1;
        }
        return N;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}