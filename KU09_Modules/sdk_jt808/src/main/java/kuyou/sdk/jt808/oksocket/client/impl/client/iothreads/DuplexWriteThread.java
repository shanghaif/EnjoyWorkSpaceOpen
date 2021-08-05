package kuyou.sdk.jt808.oksocket.client.impl.client.iothreads;

import kuyou.sdk.jt808.oksocket.client.impl.exceptions.ManuallyDisconnectException;
import kuyou.sdk.jt808.oksocket.client.sdk.client.action.IAction;
import kuyou.sdk.jt808.oksocket.core.iocore.interfaces.IStateSender;
import kuyou.sdk.jt808.oksocket.core.iocore.interfaces.IWriter;
import kuyou.sdk.jt808.oksocket.core.utils.SLog;
import kuyou.sdk.jt808.oksocket.interfaces.basic.AbsLoopThread;

import java.io.IOException;

/**
 * Created by xuhao on 2017/5/17.
 */

public class DuplexWriteThread extends AbsLoopThread {
    private IStateSender mStateSender;

    private IWriter mWriter;

    public DuplexWriteThread(IWriter writer,
                             IStateSender stateSender) {
        super("client_duplex_write_thread");
        this.mStateSender = stateSender;
        this.mWriter = writer;
    }

    @Override
    protected void beforeLoop() {
        mStateSender.sendBroadcast(IAction.ACTION_WRITE_THREAD_START);
    }

    @Override
    protected void runInLoopThread() throws IOException {
        mWriter.write();
    }

    @Override
    public synchronized void shutdown(Exception e) {
        mWriter.close();
        super.shutdown(e);
    }

    @Override
    protected void loopFinish(Exception e) {
        e = e instanceof ManuallyDisconnectException ? null : e;
        if (e != null) {
            SLog.e("duplex write error,thread is dead with exception:" + e.getMessage());
        }
        mStateSender.sendBroadcast(IAction.ACTION_WRITE_THREAD_SHUTDOWN, e);
    }
}
