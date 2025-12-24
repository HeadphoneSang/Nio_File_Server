package org.template.server.components.internals;

import org.template.server.components.pojo.Future;
import org.template.server.components.abstracts.PromiseListener;

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

    protected abstract void setSuccess();

    protected abstract void setFailure();

    protected void setSuccess0(){
        this.setSuccess();
        Future future = new Future();
        future.setSuccess(true);
        for (PromiseListener listener :listeners){
            listener.onStateChange(future);
        }
    }

    protected void setFailure0(){
        this.setFailure();
        Future future = new Future();
        future.setSuccess(false);
        for (PromiseListener listener :listeners){
            listener.onStateChange(future);
        }
    }
}
