package org.template.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public class PreWriteHandler implements ClientWriteHandler{

    private final NServer server;

    private final Logger logger;

    public PreWriteHandler(NServer server){
        this.server = server;
        this.logger = LoggerFactory.getLogger(server.getClass());
    }
    @Override
    public boolean serve(SocketChannel socketChannel, ExecutorService workPool, HashMap<SocketChannel, ByteBuffer> channelMap) throws IOException {
        logger.info("处理业务");
        ByteBuffer buffer = channelMap.get(socketChannel);
        try {
            int readLen = socketChannel.read(buffer);  //先把OS缓冲区的读到用户内存里
            if (readLen == -1){  //tcp连接断开
                logger.info("Fin From Client,Close Connect");
                socketChannel.close();
                channelMap.remove(socketChannel);
                return false;
            }
            buffer.flip();
            while (buffer.remaining() >= (4*2)){ //至少大于两个字节说明可能有完整的TCP帧
                buffer.mark();
                byte[] cache = new byte[4];
                buffer.get(cache,0,4);  //大端的bytes数组。检查一下需不需要反转
                int op_code = ((cache[0] & 0xff) << 3*8) | ((cache[1] & 0xff) << 2*8) | ((cache[2] & 0xff) << 8) | (cache[3] & 0xff);
                logger.info("op_code:"+op_code);
                buffer.get(cache,0,4);  //大端的bytes数组。检查一下需不需要反转
                int dataLen = ((cache[0] & 0xff) << 3*8) | ((cache[1] & 0xff) << 2*8) | ((cache[2] & 0xff) << 8) | (cache[3] & 0xff);
                if (buffer.remaining()<dataLen){  //数据部分还没传过来，等下次传过来在解析
                    buffer.reset();
                    break;
                }
                byte[] dataCache = new byte[dataLen];
                buffer.get(dataCache,0,dataLen);
                logger.info(new String(dataCache, StandardCharsets.UTF_8));
            }
            buffer.compact();
        } catch (IOException e) {
            logger.info("RST From Client,Close Connect");
            buffer.clear();
            channelMap.remove(socketChannel);
            socketChannel.close();
            return false;
        }
        return true;
    }
}
