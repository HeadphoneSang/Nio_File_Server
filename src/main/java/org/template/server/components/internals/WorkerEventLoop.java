package org.template.server.components.internals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class WorkerEventLoop extends EventLoop{

    /***
     * handler read event
     * prevent ET activation bug
     * @param keyEvent socket
     * @param pipe handler_list
     */
    private void handlerReadEvent(SelectionKey keyEvent,BasePipeline pipe){
        SocketChannel channel = (SocketChannel) keyEvent.channel();
        ByteBuffer buffer = pipe.allocateBuffer();
        ByteBuffer channelBuffer;
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
                channelBuffer = buffer.asReadOnlyBuffer();
                // 虽然是更换了引用，但是和buffer的虚拟内存空间是同一个范围，所以当修改buffer的数据时，channelBuffer的内容也会变
                // 所以管道内对channelBuffer的异步读取，会导致数据丢失，需要管道自己保存一份数据副本
                channelBuffer.flip();
                pipe.fireHandlersFromBegin(channel,channelBuffer);
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
        this.channelMap.remove(channel);
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void loopEpoch(SelectionKey keyEvent, List<Map.Entry<SelectionKey, BasePipeline>> curPipes) {
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
