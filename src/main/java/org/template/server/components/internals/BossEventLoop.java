package org.template.server.components.internals;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;

public class BossEventLoop extends EventLoop{

//    private void acceptConnection() throws IOException {
//        SocketChannel client = this.serverChannel.accept();
//        client.configureBlocking(false);
//
//    }

    @Override
    protected void loopEpoch(SelectionKey keyEvent, List<Map.Entry<SelectionKey, BasePipeline>> curPipes) {
        if (keyEvent.isAcceptable()){
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) keyEvent.channel();
            BasePipeline pipeline = this.channelMap.get(serverSocketChannel);
            try {
                SocketChannel client = serverSocketChannel.accept();
                pipeline.fireHandlersFromBegin(client,client);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
