package org.template;

import org.template.server.components.internals.*;
import org.template.server.components.handlers.ByteEncodeHandler;
import org.template.server.components.handlers.DecisionHandler;
import org.template.server.components.handlers.SimpleBufferDecodeHandler;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        ServerBootStrap serverBootStrap = new ServerBootStrap();
        BossEventLoopGroup bossGroup = new BossEventLoopGroup(1);
        WorkerEventLoopGroup workerGroup = new WorkerEventLoopGroup(4);
        serverBootStrap.group(bossGroup,workerGroup)
                .localAddress(new InetSocketAddress(25565))
                .childHandlers(()->{
                    BasePipeline pipeline = new BasePipeline(true,5*1024*1024);
                    pipeline.
                            addLast("encodeHandler",new ByteEncodeHandler()).
                            addLast("simpleHandler",new SimpleBufferDecodeHandler()).
                            addLast("decision",new DecisionHandler());
                    return pipeline;
                });
        ChannelPromise promise = serverBootStrap.bind();
        promise.addListener((future)->{
            if (future.isSuccess()){
            }
        });
    }
}