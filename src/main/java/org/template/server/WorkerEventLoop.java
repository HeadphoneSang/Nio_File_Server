package org.template.server;

import org.template.server.components.BasePipeline;
import org.template.server.components.InitPipeline;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WorkerEventLoop extends EventLoop{
    WorkerEventLoop(InitPipeline initPipeHandler) {
        super(initPipeHandler);
    }

    /***
     * handler read event
     * prevent ET activation bug
     * @param keyEvent socket
     * @param pipe handler_list
     */
    private void handlerReadEvent(SelectionKey keyEvent,BasePipeline pipe){
        SocketChannel channel = (SocketChannel) keyEvent.channel();
        ByteBuffer buffer = pipe.allocateBuffer();
        int readLen;
        do{
            try {
                readLen = channel.read(buffer);
            } catch (IOException e) {
                this.onInterrupt(keyEvent,pipe);
                logger.warn(e.getMessage());
                return;
            }
            if (readLen==-1)
            {
                /**
                 * Todo 处理断开的连接
                 */
                this.onInterrupt(keyEvent,pipe);
                return;
            }
            else if (readLen > 0){
                pipe.fireHandlersFromBegin(channel,buffer);
            }
            buffer.clear();
        }while (readLen > 0);
    }

    /**
     * 连接中断时的处理逻辑
     * @param keyEvent 断开连接的连接对象
     * @param pipe 对应的处理管道
     */
    private void onInterrupt(SelectionKey keyEvent,BasePipeline pipe){
        pipe.fireInterrupt();
        keyEvent.cancel();
        SocketChannel channel = (SocketChannel) keyEvent.channel();
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void loopEpoch(Iterator<SelectionKey> readyEvents, List<Map.Entry<SelectionKey, BasePipeline>> curPipes) {
        while(readyEvents.hasNext()){
            SelectionKey keyEvent = readyEvents.next();
            readyEvents.remove();
            if(keyEvent.isReadable()){
                BasePipeline pipe = channelMap.get((SocketChannel) keyEvent.channel());
                handlerReadEvent(keyEvent,pipe);
                curPipes.add(new AbstractMap.SimpleEntry<>(keyEvent,pipe));
            }else if(keyEvent.isWritable()){
                BasePipeline pipe = channelMap.get((SocketChannel) keyEvent.channel());
                curPipes.add(new AbstractMap.SimpleEntry<>(keyEvent,pipe));
            }
        }
    }
}
