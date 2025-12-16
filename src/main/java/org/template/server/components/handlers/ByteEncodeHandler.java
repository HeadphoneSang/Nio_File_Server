package org.template.server.components.handlers;

import org.template.server.components.HandlerContext;
import org.template.server.components.WritePromise;
import org.template.server.components.pojo.BufferPack;
import org.template.server.utils.EncodeUtils;

import java.nio.ByteBuffer;

public class ByteEncodeHandler extends SimpleHandler<Object, BufferPack>{
    @Override
    public void write(HandlerContext ctx, WritePromise promise, BufferPack msg) {
        int dataLen = 0;
        byte[] opBytes = EncodeUtils.encodeToBigBytes(msg.getOperator(),4);
        if(msg.getData()!=null){
            dataLen = msg.getData().remaining();
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(4+8+dataLen);
        byte[] lenBytes = EncodeUtils.encodeToBigBytes(dataLen,8);
        buffer.put(opBytes);
        buffer.put(lenBytes);
        if (dataLen!=0)
            buffer.put(msg.getData());
        buffer.flip();
        ctx.fireNextWriteHandler(buffer,promise);
    }

    @Override
    public void onRemoved(HandlerContext ctx) {

    }

    @Override
    public void onAdded(HandlerContext ctx) {
        ctx.disableRead();
        ctx.enableWrite();
    }

    @Override
    public void onDestroy(HandlerContext ctx) {

    }

    @Override
    public void channelRead0(HandlerContext ctx, Object msg) {

    }
}
