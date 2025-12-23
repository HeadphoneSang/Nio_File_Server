package org.template.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.template.server.components.BasePipeline;
import org.template.server.components.InitPipeline;
import org.template.server.components.abstracts.RegisterTask;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class EventLoop implements Runnable{

    private final AtomicBoolean cancelled  = new AtomicBoolean(false);

    private final Queue<RegisterTask> registerTasks = new ConcurrentLinkedDeque<>();

    protected final HashMap<SocketChannel, BasePipeline> channelMap = new HashMap<>();

    protected static final Logger logger = LoggerFactory.getLogger(NServer.class);

    protected final InitPipeline initPipeHandler;

    protected final Selector selector;

    EventLoop(InitPipeline initPipeHandler){
        try {
            selector = Selector.open();
            this.initPipeHandler = initPipeHandler;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void cancel(){
        this.cancelled.set(true);
    }

    protected void regEventTask(SocketChannel client){
        registerTasks.add(()->{
            try {
                client.configureBlocking(false);
                this.channelMap.put(client, initPipeHandler.init());
                this.channelMap.get(client).setLogger(logger);
                client.register(selector, SelectionKey.OP_READ);
                logger.info("new tcp link registered: " + client.getRemoteAddress());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void run(){
        try{
            if (Thread.currentThread().isInterrupted() || cancelled.get()) {
                return;
            }
            execute();
        }catch (Throwable e){
            logger.error(e.getMessage());
        }finally {
            for (Map.Entry<SocketChannel,BasePipeline> entry : channelMap.entrySet()){
                SocketChannel client = entry.getKey();
                BasePipeline pipeline = entry.getValue();
                try {
                    client.close();
                    client.keyFor(selector).cancel();
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
            loopEpoch(readyEvents,curPipes);
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

    protected abstract void loopEpoch(Iterator<SelectionKey> readyEvents,List<Map.Entry<SelectionKey,BasePipeline>> curPipes);
}
