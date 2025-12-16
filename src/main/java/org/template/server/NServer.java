package org.template.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.template.server.components.BasePipeline;
import org.template.server.components.InitPipeline;
import org.template.server.components.pojo.PromiseEntry;

public class NServer {
    private final int port;
    private final HashMap<SocketChannel, BasePipeline> channelMap;

    private ServerSocketChannel serverChannel;
    private static final Logger logger = LoggerFactory.getLogger(NServer.class);

    private InitPipeline initPipeHandler;

    private Selector selector;
    private final ExecutorService workerPool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    public NServer(int port){
        this.port = port;
        this.channelMap = new HashMap<>();
    }

    public int getPort() {
        return port;
    }

    private void acceptConnection() throws IOException {
        SocketChannel client = this.serverChannel.accept();
        client.configureBlocking(false);
        this.channelMap.put(client, initPipeHandler.init());
        this.channelMap.get(client).setLogger(logger);
        client.register(selector, SelectionKey.OP_READ);
        logger.info("新连接到来: " + client.getRemoteAddress());
    }

    /**
     * 连接中断时的处理逻辑
     * @param keyEvent 断开连接的连接对象
     * @param pipe 对应的处理管道
     */
    private void onInterrupt(SelectionKey keyEvent,BasePipeline pipe){
        /**
         * 如果是正常断开，客户端->fin，ack->客户端，执行onInterrupt，我们只要返回一个fin，也就是close就行了。
         * 如果是异常断开，也就是客户端RST了，
         */
        pipe.fireInterrupt();
        keyEvent.cancel();
        SocketChannel channel = (SocketChannel) keyEvent.channel();
        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    public void accept(InitPipeline initPipeline) throws IOException {
        this.initPipeHandler = initPipeline;
        try {
            this.serverChannel = ServerSocketChannel.open();
            this.serverChannel.bind(new InetSocketAddress(this.port));
            this.selector = Selector.open();
            this.serverChannel.configureBlocking(false);
            this.serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            logger.info("服务器已启动:"+this.serverChannel.getLocalAddress());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Map.Entry<SelectionKey,BasePipeline>> curPipes = new ArrayList<>();
        while(true){
            selector.select();
            Iterator<SelectionKey> readyEvents = selector.selectedKeys().iterator();
            curPipes.clear();
            while(readyEvents.hasNext()){
                SelectionKey keyEvent = readyEvents.next();
                readyEvents.remove();
                if (keyEvent.isAcceptable()){
                    this.acceptConnection();
                }else if(keyEvent.isReadable()){
                    BasePipeline pipe = channelMap.get((SocketChannel) keyEvent.channel());
                    handlerReadEvent(keyEvent,pipe);
                    curPipes.add(new AbstractMap.SimpleEntry<>(keyEvent,pipe));
                }else if(keyEvent.isWritable()){
                    BasePipeline pipe = channelMap.get((SocketChannel) keyEvent.channel());
                    curPipes.add(new AbstractMap.SimpleEntry<>(keyEvent,pipe));
                }
            }
            //处理io事件
            for (Map.Entry<SelectionKey, BasePipeline> entry : curPipes){
                entry.getValue().doWrite(entry.getKey());
            }
        }
    }
}
