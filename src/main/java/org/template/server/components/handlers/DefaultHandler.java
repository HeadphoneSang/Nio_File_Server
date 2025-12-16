package org.template.server.components.handlers;

import org.template.server.components.HandlerContext;
import org.template.server.components.WritePromise;

import java.nio.ByteBuffer;

public class DefaultHandler extends SimpleHandler<ByteBuffer,ByteBuffer>{
    @Override
    public void write(HandlerContext ctx, WritePromise promise, ByteBuffer msg) {
        ctx.getPipeline().write(msg,promise);
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
    public void channelRead0(HandlerContext ctx, ByteBuffer msg) {

    }
}
