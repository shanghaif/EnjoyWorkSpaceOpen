package kuyou.common.ku09;

import android.app.Application;
import android.app.HelmetModuleManageServiceManager;
import android.app.IHelmetModuleCommonCallback;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import kuyou.common.exception.IGlobalExceptionControl;
import kuyou.common.exception.UncaughtExceptionManager;
import kuyou.common.ipc.RemoteEvent;
import kuyou.common.ipc.RemoteEventBus;
import kuyou.common.ku09.config.DeviceConfig;
import kuyou.common.ku09.event.IDispatchEventCallback;
import kuyou.common.ku09.event.common.EventKeyClick;
import kuyou.common.ku09.event.common.EventKeyDoubleClick;
import kuyou.common.ku09.event.common.EventKeyLongClick;
import kuyou.common.ku09.event.common.EventPowerChange;
import kuyou.common.ku09.event.tts.EventTextToSpeechPlayRequest;
import kuyou.common.ku09.handler.BasicEventHandler;
import kuyou.common.ku09.handler.HandlerStatusGuard;
import kuyou.common.ku09.handler.basic.IStatusGuardCallback;
import kuyou.common.ku09.handler.basic.StatusGuardRequestConfig;
import kuyou.common.log.LogcatHelper;
import kuyou.common.utils.CommonUtils;
import kuyou.common.utils.DebugUtil;
import kuyou.common.utils.SystemPropertiesUtils;

/**
 * action :模块通用基础实现[抽象]
 * <p>
 * author: wuguoxian <br/>
 * date: 20-11-4 <br/>
 * 已实现列表：<br/>
 * 1 IPC框架配置 <br/>
 * 2 log保存 <br/>
 * 3 模块活动保持 <br/>
 * 4 设备基础配置 <br/>
 * 5 设备部分状态监听 <br/>
 * 6 按键监听分发 <br/>
 * <p>
 */
public abstract class BasicModuleApplication extends Application implements
        IDispatchEventCallback,
        IModuleManager {

    protected String TAG = "kuyou.common.ku09 > BasicModuleApplication";

    protected HelmetModuleManageServiceManager mHelmetModuleManageServiceManager;

    @Override
    public final void onCreate() {
        super.onCreate();
        init();
    }

    // =========================== 初始化 ==============================

    protected void init() {
        TAG = new StringBuilder(getPackageName()).append(" > ModuleApplication").toString();

        //模块间IPC框架初始化
        RemoteEventBus.getInstance(getApplicationContext())
                .register(new RemoteEventBus.IRegisterConfig() {
                    @Override
                    public RemoteEventBus.IFrameLiveListener getFrameLiveListener() {
                        return BasicModuleApplication.this.getIpcFrameLiveListener();
                    }

                    @Override
                    public List<Integer> getEventDispatchList() {
                        return BasicModuleApplication.this.getEventDispatchList();
                    }

                    @Override
                    public Object getLocalEventDispatchHandler() {
                        return BasicModuleApplication.this;
                    }
                });

        //StrictMode相关
        initStrictModePolicy();

        //log相关
        initLogcatLocal();
        initExceptionLogLocal();

        //初始化模块状态控制系统服务
        initHelmetModuleManageServiceManager();
        initCallBack();

        //初始化按键协处理器
        initKeyHandlers();
    }

    /**
     * action:初始化严格模式配置
     */
    protected void initStrictModePolicy() {
        final String key = "persist.hm.strict.mode";
        if (!SystemPropertiesUtils.get(key, "0").equals("1")) {
            Log.d(TAG, "initLogcatLocal > LogcatHelper is disable");
            return;
        }
        DebugUtil.startStrictModeThreadPolicy();
        DebugUtil.startStrictModeVmPolicy();
    }

    /**
     * action:初始化log本地保存
     */
    protected void initLogcatLocal() {
        final String key = "persist.hm.log.save";
        if (!SystemPropertiesUtils.get(key, "0").equals("1")) {
            Log.d(TAG, "initLogcatLocal > LogcatHelper is disable");
            return;
        }
        LogcatHelper.getInstance(getApplicationContext())
                .setSaveLogDirPath(new StringBuilder()
                        .append("/kuyou/logcat/")
                        .append(getApplicationName())
                        .toString())
                .setLogSizeMax(1024 * 1024 * 10) //100M
                .start("logcat \"*:i*:w*:e\" | grep \"(" + android.os.Process.myPid() + ")\"");
    }

    /**
     * action:初始化异常log本地保存
     */
    protected void initExceptionLogLocal() {
        final String key = "persist.hm.exception.save";
        if (!SystemPropertiesUtils.get(key, "0").equals("1")) {
            Log.d(TAG, "initExceptionLogLocal > exception info auto save is disable");
            return;
        }
        UncaughtExceptionManager uem = UncaughtExceptionManager
                .getInstance(new IGlobalExceptionControl() {
                    @Override
                    public Application getApplication() {
                        return BasicModuleApplication.this;
                    }

                    @Override
                    public int getPolicy() {
                        int flags = 0;
                        flags |= IGlobalExceptionControl.POLICY_ENABLE_EXIT_APP;
                        flags |= IGlobalExceptionControl.POLICY_ENABLE_CRASH_PROMPT;
                        return flags;
                    }
                })
                .setSaveExceptionLogDirPath(new StringBuilder()
                        .append("/kuyou/logcat/")
                        .append(getApplicationName())
                        .toString());
    }

    /**
     * action:初始化模块服务回调
     */
    protected void initCallBack() {

        StringBuilder statusInfo = new StringBuilder().append("======================================================\n    ");

        statusInfo.append("\n模块：").append(getApplicationName());
        statusInfo.append("\n版本：").append(BuildConfig.BUILD_DATE);

        long timeNow = System.currentTimeMillis() + 8 * 3600 * 1000;
        long time = timeNow;
        try {
            time = CommonUtils.formatDate2Stamp(BuildConfig.BUILD_DATE, BuildConfig.BUILD_DATE_PATTERN);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        if (timeNow > time && timeNow - time < 600000) {
            statusInfo.append("\n版本状态：").append("新鲜");
        } else {
            statusInfo.append("\n版本状态：").append("原始");
        }
        statusInfo.append("\n状态：").append("模块启动");
        statusInfo.append("\n操作：").append("注册回调");

        Log.i(TAG, statusInfo.append("    \n\n======================================================")
                .toString());
    }

    protected void initHelmetModuleManageServiceManager() {
        if (null != mHelmetModuleManageServiceManager) {
            return;
        }
        mHelmetModuleManageServiceManager = (HelmetModuleManageServiceManager) getSystemService("helmet_module_manage_service");
    }

    protected void initKeyHandlers() {
        mHelmetModuleManageServiceManager.registerHelmetModuleCommonCallback(new IHelmetModuleCommonCallback.Stub() {

            @Override
            public void onPowerStatus(int status) throws RemoteException {
                BasicModuleApplication.this.dispatchEvent(new EventPowerChange().setPowerStatus(status));
            }

            @Override
            public void onKeyClick(int keyCode) throws RemoteException {
                BasicModuleApplication.this.dispatchEvent(new EventKeyClick(keyCode));
            }

            @Override
            public void onKeyDoubleClick(int keyCode) throws RemoteException {
                BasicModuleApplication.this.dispatchEvent(new EventKeyDoubleClick(keyCode));
            }

            @Override
            public void onKeyLongClick(int keyCode) throws RemoteException {
                BasicModuleApplication.this.dispatchEvent(new EventKeyLongClick(keyCode));
            }
        });
    }

    // ============================ 模块状态看门狗 ============================

    private static final int FLAG_FEED_TIME_LONG = 25 * 1000;
    private int mStatusGuardCallbackFlag = -1;
    private HandlerStatusGuard mHandlerStatusGuard;

    protected HandlerStatusGuard getHandlerStatusGuard() {
        if (null == mHandlerStatusGuard) {
            mHandlerStatusGuard = HandlerStatusGuard.getSingleton();
            mHandlerStatusGuard.registerStatusGuardCallback(new IStatusGuardCallback() {
                @Override
                public void onReceiveMessage() {
                    BasicModuleApplication.this.onFeedWatchDog();
                }

                @Override
                public void onRemoveMessage() {

                }

                @Override
                public void setReceiveMessage(int what) {
                    BasicModuleApplication.this.mStatusGuardCallbackFlag = what;
                }
            }, new StatusGuardRequestConfig(true, getFeedTimeLong(), Looper.getMainLooper()));
            mHandlerStatusGuard.start(mStatusGuardCallbackFlag);
        }
        return mHandlerStatusGuard;
    }

    /**
     * action:模块状态看门狗 > 相关状态检测的流程的处理
     */
    protected void onFeedWatchDog() {
        Log.d(TAG, "onFeedWatchDog > MSG_WATCHDOG_2_FEED ");

        String status = isReady();

        if (null == status || status.replaceAll(" ", "").length() == 0) {
            //提醒boss自己还没挂,和运行状态
            mHelmetModuleManageServiceManager.feedWatchDog(getPackageName(), System.currentTimeMillis());
        } else {
            onDogBitesLazyBug(-1, status);
        }
    }

    /**
     * action:模块状态看门狗 > 相关状态检测的流程的周期长度,单位毫秒
     */
    protected long getFeedTimeLong() {
        return FLAG_FEED_TIME_LONG;
    }

    /**
     * action:模块状态看门狗 > 模块返回活动状态
     */
    protected String isReady() {
        boolean isReady = RemoteEventBus.getInstance(getApplicationContext()).isRegister(getPackageName());
        if (!isReady) {
            return "远程模块框架未初始化完成";
        }
        return null;
    }

    /**
     * action:模块状态看门狗 > 模块在偷懒,抓起来打一顿
     *
     * @param flag 重启的等待时间,毫秒
     */
    protected void onDogBitesLazyBug(int flag, String stasMsg) {
        StringBuilder logInfo = new StringBuilder()
                .append("======================================================\n")
                .append("\n模块：").append(getApplicationName())
                .append("\n异常状态:").append(stasMsg)
                .append("\n操作：");
        if (-1 != flag) {
            flag = Math.abs(flag) < 5000 ? 5000 : flag;
            logInfo.append("在").append(flag).append("毫秒后重启");
        } else {
            logInfo.append("重启模块");
        }
        Log.e(TAG, logInfo.append("\n\n======================================================").toString());

        getHelmetModuleManageServiceManager().feedWatchDog(getPackageName(), -flag);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void reboot(int delayedMillisecond) {
        getHelmetModuleManageServiceManager().rebootModule(
                getPackageName(),
                android.os.Process.myPid(),
                delayedMillisecond);
    }

    // ============================ 模块间IPC，模块与系统服务 ,模块事件的各种协处理器 ============================

    private RemoteEventBus.IFrameLiveListener mFrameLiveListener;

    private List<BasicEventHandler> mEventHandlerList = null;

    /**
     * action:注册事件处理器
     **/
    protected abstract void initRegisterEventHandlers();

    /**
     * action:远程事件的监听列表
     **/
    protected List<Integer> getEventDispatchList() {
        if (0 == getEventHandlerList().size()) {
            Log.e(TAG, "getEventDispatchList > process fail : handlers is null");
            return null;
        }
        return getEventDispatchListByHandlers(getEventHandlerList());
    }

    private List<Integer> getEventDispatchListByHandlers(List<BasicEventHandler> handlerList) {
        List<Integer> codeList = new ArrayList<>();
        for (BasicEventHandler handler : handlerList) {
            handler.setContext(BasicModuleApplication.this);
            handler.setDispatchEventCallBack(BasicModuleApplication.this);
            handler.setModuleManager(BasicModuleApplication.this);
            handler.setDevicesConfig(getDeviceConfig());
            handler.setStatusGuardHandler(getHandlerStatusGuard());
            if (null != handler.getSubEventHandlers()) {
                codeList.addAll(getEventDispatchListByHandlers(handler.getSubEventHandlers()));
            }
            codeList.addAll(handler.getHandleRemoteEventCodeList());
        }
        return codeList;
    }

    /**
     * action:模块间IPC框架状态监听器
     **/
    protected RemoteEventBus.IFrameLiveListener getIpcFrameLiveListener() {
        if (null == mFrameLiveListener) {
            mFrameLiveListener = new RemoteEventBus.IFrameLiveListener() {
                @Override
                public void onIpcFrameResisterSuccess() {
                    Log.d(TAG, "onIpcFrameResisterSuccess > ");
                }

                @Override
                public void onIpcFrameUnResister() {
                    Log.d(TAG, "onIpcFrameUnResister > ");
                }
            };
        }
        return mFrameLiveListener;
    }

    protected BasicModuleApplication registerEventHandler(BasicEventHandler handler) {
        getEventHandlerList().add(handler);
        return BasicModuleApplication.this;
    }

    protected List<BasicEventHandler> getEventHandlerList() {
        if (null == mEventHandlerList) {
            mEventHandlerList = new ArrayList<>();
            initRegisterEventHandlers();
        }
        return mEventHandlerList;
    }

    @Override
    public void dispatchEvent(RemoteEvent event) {
        RemoteEventBus.getInstance().dispatch(event);
    }

    //本地事件
    @Subscribe
    public void onModuleEvent(RemoteEvent event) {
        for (BasicEventHandler handler : getEventHandlerList()) {
            handler.onModuleEvent(event);
//            if (handler.onModuleEvent(event)) {
//                Log.d(TAG, "已消费 event = " + event.getCode());
//                Log.d(TAG, "EventHandler = " + handler.getClass().getSimpleName());
//                return;
//            }
        }
        Log.i(TAG, "onModuleEvent > unable to consumption event = " + event.getCode());
    }

    public void play(String content) {
        if (null == content || content.length() <= 0) {
            Log.e(TAG, "play > process fail : content is invalid");
            return;
        }
        dispatchEvent(new EventTextToSpeechPlayRequest(content));
    }

    // =========================== 设备配置等==============================

    protected abstract String getApplicationName();

    private DeviceConfig mDeviceConfig;

    public DeviceConfig getDeviceConfig() {
        if (null == mDeviceConfig) {
            mDeviceConfig = new DeviceConfig();
        }
        return mDeviceConfig;
    }

    public HelmetModuleManageServiceManager getHelmetModuleManageServiceManager() {
        initHelmetModuleManageServiceManager();
        return mHelmetModuleManageServiceManager;
    }
}
