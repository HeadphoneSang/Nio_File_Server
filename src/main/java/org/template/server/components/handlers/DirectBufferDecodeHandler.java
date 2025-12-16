package org.template.server.components.handlers;

import org.template.server.components.HandlerContext;
import org.template.server.components.WritePromise;
import org.template.server.components.pojo.ObjPack;
import org.template.server.utils.DecodeUtils;

import java.nio.ByteBuffer;

public class DirectBufferDecodeHandler extends SimpleHandler<ByteBuffer,Object> {
    private final ByteBuffer buffer;

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
        src.flip();
        this.buffer.put(src);
        src.compact();
    }

    @Override
    public void onDestroy(HandlerContext ctx) {

    }

    @Override
    public void channelRead0(HandlerContext ctx, ByteBuffer msg) {
        readMsg(msg);
        this.buffer.flip();
        byte[] b4 = new byte[4];
        byte[] b8 = new byte[8];
        ObjPack datapack;
        while(this.buffer.remaining() >= 12){
            this.buffer.mark();
            this.buffer.get(b4,0,4);
            int opCode = DecodeUtils.readBEInt(b4);
            this.buffer.get(b8,0,8);
            long dataLen = DecodeUtils.readBELong(b8);
            if (this.buffer.remaining() >= dataLen){
                byte[] data = new byte[(int) dataLen];
                this.buffer.get(data,0, (int) dataLen);
                datapack = new ObjPack(opCode,data);
                ctx.fireNextReadHandler(datapack);
            }else{
                this.buffer.reset();
                break;
            }
        }
        this.buffer.compact();  //switch write model
    }

}
