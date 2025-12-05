package org.template.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.template.server.components.BasePipeline;
import org.template.server.components.InitPipeline;

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

    /***
     * handler read event
     * prevent ET activation bug
     * @param channel socket
     * @param pipe handler_list
     */
    private void handlerReadEvent(SocketChannel channel,BasePipeline pipe) throws IOException {
        ByteBuffer buffer = pipe.allocateBuffer();
        int readLen;
        do{
            readLen = channel.read(buffer);
            if (readLen==-1)
            {
                /**
                 * Todo 处理断开的连接
                 */
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
        while(true){
            selector.select();
            Iterator<SelectionKey> readyEvents = selector.selectedKeys().iterator();
            while(readyEvents.hasNext()){
                SelectionKey keyEvent = readyEvents.next();
                readyEvents.remove();
                if (keyEvent.isAcceptable()){
                    this.acceptConnection();
                }else if(keyEvent.isReadable()){
                    BasePipeline pipe = channelMap.get((SocketChannel) keyEvent.channel());
                    handlerReadEvent((SocketChannel) keyEvent.channel(),pipe);
                }
            }
        }
    }
}
