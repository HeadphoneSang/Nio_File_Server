package org.template.server.components;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BufferHandler extends SimpleHandler<ByteBuffer> {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024*64);

    @Override
    void onRemoved(HandlerContext ctx) {

    }

    @Override
    void onAdded(HandlerContext ctx) {
        ctx.enableRead();
        ctx.enableWrite();
    }

    private void readMsg(ByteBuffer src){
        src.flip();
        this.buffer.put(src);
        src.compact();
    }

    private int readBEInt(byte[] b4){
        return ((b4[0] & 0xff) << 3*8) | ((b4[1] & 0xff) << 2*8) | ((b4[2] & 0xff) << 8) | ((b4[3] & 0xff));
    }

    @Override
    void channelRead0(HandlerContext ctx, ByteBuffer msg) {
        readMsg(msg);
        this.buffer.flip();
        byte[] b4 = new byte[4];
        while(this.buffer.remaining() >= 4*2){
            this.buffer.mark();
            this.buffer.get(b4,0,4);
            int opCode = readBEInt(b4);
            this.buffer.get(b4,0,4);
            int dataLen = readBEInt(b4);
            if (this.buffer.remaining() >= dataLen){
                byte[] data = new byte[dataLen];
                this.buffer.get(data,0,dataLen);
                ctx.fireNextReadHandler(new String(data,StandardCharsets.UTF_8));
            }else{
                this.buffer.reset();
                break;
            }
        }
        this.buffer.compact();  //switch write model
    }

    @Override
    void channelWrite0(HandlerContext ctx, ByteBuffer msg) {

    }
}
