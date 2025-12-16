package org.template.server.components;

import org.template.server.components.pojo.Future;
import org.template.server.components.pojo.PromiseListener;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class ChannelPromise {
    private final List<PromiseListener> listeners = new ArrayList<>();

    public ChannelPromise(){
    }

    public ChannelPromise addListener(PromiseListener listener){
        listeners.add(listener);
        return this;
    }

    public abstract void setSuccess();

    public abstract void setFailure();

    public void setSuccess0(){
        this.setSuccess();
        Future future = new Future();
        future.setSuccess(true);
        for (PromiseListener listener :listeners){
            listener.onStateChange(future);
        }
    }

    public void setFailure0(){
        this.setFailure();
        Future future = new Future();
        future.setSuccess(false);
        for (PromiseListener listener :listeners){
            listener.onStateChange(future);
        }
    }
}
