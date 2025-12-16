package org.template.server.components.pojo;

import org.template.server.components.ChannelPromise;

public class PromiseEntry <T>{
    private final T payload;

    private final ChannelPromise promise;

    public PromiseEntry(T payload,ChannelPromise promise){
        this.payload = payload;
        this.promise = promise;
    }

    public T getPayload() {
        return payload;
    }

    public ChannelPromise getPromise() {
        return promise;
    }
}
