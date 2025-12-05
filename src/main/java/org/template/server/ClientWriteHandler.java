package org.template.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public interface ClientWriteHandler {
    /**
     * 客户端数据处理
     * @param socketChannel 客户端通道
     * @param workPool 工作池
     * @param channelMap Buffer表
     */
    boolean serve(SocketChannel socketChannel, ExecutorService workPool, HashMap<SocketChannel, ByteBuffer> channelMap) throws IOException;
}
