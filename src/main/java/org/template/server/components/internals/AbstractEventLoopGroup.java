package org.template.server.components.internals;

import org.template.server.components.abstracts.InitPipeline;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

public abstract class AbstractEventLoopGroup {

    protected int nThreads;

    protected EventLoop[] eventLoops;

    protected ThreadFactory factory;

    protected InitPipeline initPipeline;

    public AbstractEventLoopGroup(int nThreads){
        this.nThreads = nThreads;
    }

    /**
     * 在group管理的所有loop中负载均衡的选择一个loop返回
     * @return 返回一个loop
     */
    protected abstract EventLoop next();

    public ChannelPromise register(SocketChannel channel){
        EventLoop loop = next();
        return loop.regEventTask(channel);
    }
    public ChannelPromise register(ServerSocketChannel serverSocketChannel){
        EventLoop loop = next();
        return loop.regEventTask(serverSocketChannel);
    }

    public void setInitPipeline(InitPipeline pipeline){
        this.initPipeline = pipeline;
    }


}
