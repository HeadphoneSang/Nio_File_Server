package org.template.server.components.internals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.template.server.components.abstracts.InitPipeline;
import org.template.server.components.abstracts.RegisterTask;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class EventLoop implements Runnable{

    private ThreadFactory factory;

    private final AtomicBoolean cancelled  = new AtomicBoolean(false);

    private final Queue<RegisterTask> registerTasks = new ConcurrentLinkedDeque<>();

    protected final HashMap<AbstractSelectableChannel, BasePipeline> channelMap = new HashMap<>();

    protected static final Logger logger = LoggerFactory.getLogger(EventLoop.class);

    private volatile Thread thread;

    protected InitPipeline initPipeHandler;

    protected Selector selector;

    EventLoop(InitPipeline initPipeHandler){
        this.initPipeHandler = initPipeHandler;
    }

    EventLoop(){
        this(null);
    }

    public void setInitPipeHandler(InitPipeline initPipeHandler){
        this.initPipeHandler = initPipeHandler;
    }

    public void initSelector(){
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void cancel(){
        this.cancelled.set(true);
    }

    void regThreadFactory(ThreadFactory factory){
        this.factory = factory;
    }

    private void initThread(){
        thread = factory.newThread(this);
        thread.start();
    }

    public ChannelPromise regEventTask(SocketChannel client){
        if (selector==null){
            initSelector();
        }
        if (thread==null){
            initThread();
        }
        ChannelPromise promise = new RegisterChannelPromise();
        registerTasks.add(()->{
            try {
                client.configureBlocking(false);
                this.channelMap.put(client, initPipeHandler.init());
                this.channelMap.get(client).setLogger(logger);
                client.register(selector, SelectionKey.OP_READ);
                logger.info("new tcp link registered: " + client.getRemoteAddress());
                promise.setSuccess0();
            } catch (IOException e) {
                promise.setFailure0();
                logger.error(e.getMessage());
            }
        });
        selector.wakeup();
        return promise;
    }

    public ChannelPromise regEventTask(ServerSocketChannel server){
        if (selector==null){
            initSelector();
        }
        if(thread==null){
            initThread();
        }
        ChannelPromise promise = new RegisterChannelPromise();
        registerTasks.add(()->{
            try {
                this.channelMap.put(server, initPipeHandler.init());
                this.channelMap.get(server).setLogger(logger);
                server.configureBlocking(false);
                server.register(selector, SelectionKey.OP_ACCEPT);
                logger.info("Server listening on port:: " + server.getLocalAddress());
                promise.setSuccess0();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        selector.wakeup();
        return promise;
    }

    public void run(){
        try{
            if (Thread.currentThread().isInterrupted() || cancelled.get()) {
                return;
            }
            execute();
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            for (Map.Entry<AbstractSelectableChannel,BasePipeline> entry : channelMap.entrySet()){
                AbstractSelectableChannel channel = entry.getKey();
                BasePipeline pipeline = entry.getValue();
                try {
                    channel.close();
                    channel.keyFor(selector).cancel();
                    pipeline.fireInterrupt();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private void execute(){
        List<Map.Entry<SelectionKey,BasePipeline>> curPipes = new ArrayList<>();
        while(true){
            while (!registerTasks.isEmpty())
                registerTasks.poll().run();
            try {
                selector.select();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Iterator<SelectionKey> readyEvents = selector.selectedKeys().iterator();
            curPipes.clear();
            while(readyEvents.hasNext()) {
                SelectionKey keyEvent = readyEvents.next();
                readyEvents.remove();
                loopEpoch(keyEvent,curPipes);
            }
            //处理Executor
            for (Map.Entry<SelectionKey, BasePipeline> entry : curPipes){
                entry.getValue().doExecutors();
            }
            //处理io事件
            for (Map.Entry<SelectionKey, BasePipeline> entry : curPipes){
                try {
                    entry.getValue().doWrite(entry.getKey());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected abstract void loopEpoch(SelectionKey readyEvents,List<Map.Entry<SelectionKey,BasePipeline>> curPipes);
}
