package org.template.server.components.handlers;

import org.template.server.components.internals.HandlerContext;
import org.template.server.components.internals.WritePromise;
import org.template.server.components.pojo.*;
import org.template.server.utils.DecodeUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DirectBufferDecodeHandler extends SimpleHandler<ByteBuffer,Object> {
    private ByteBuffer buffer;

    public DirectBufferDecodeHandler(int cacheSize){
        this.buffer = ByteBuffer.allocateDirect(1024*1024*5);
    }

    @Override
    public void write(HandlerContext ctx, WritePromise promise, Object msg) {

    }

    @Override
    public void onRemoved(HandlerContext ctx) {

    }

    @Override
    public void onAdded(HandlerContext ctx) {
        ctx.enableRead();
        ctx.disableWrite();
    }

    private void readMsg(ByteBuffer src){
        this.buffer.put(src);
    }

    @Override
    public void onDestroy(HandlerContext ctx) {

    }

    @Override
    public void channelRead0(HandlerContext ctx, ByteBuffer msg) {
        while (msg.hasRemaining()){
            readMsg(msg);
            this.buffer.flip();
            byte[] b4 = new byte[4];
            byte[] b8 = new byte[8];
            ByteBuffer dataBuffer;
            while(this.buffer.remaining() >= 12){
                this.buffer.mark();
                this.buffer.get(b4,0,4);
                int opCode = DecodeUtils.readBEInt(b4);
                this.buffer.get(b8,0,8);
                int dataLen = (int)DecodeUtils.readBELong(b8);
                if (this.buffer.remaining() >= dataLen){
                    //取出当前数据部分的buffer
                    int markPos = this.buffer.position();
                    int markLimit = this.buffer.limit();
                    this.buffer.limit(markPos+dataLen);
                    dataBuffer = this.buffer.slice();
                    dataBuffer.asReadOnlyBuffer();
                    //把buffer的读取位置向前移动，跳过数据部分
                    this.buffer.position(markPos+dataLen).limit(markLimit);
                    BufferPack pack = new BufferPack(opCode,dataBuffer);
                    ctx.fireNextReadHandler(pack);
                }else{
                    this.buffer.reset();
                    break;
                }
            }
        }
        this.buffer.compact();  //switch write model
    }

}
