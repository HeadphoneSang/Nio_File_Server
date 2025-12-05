package org.template.server.components;

public class StringHandler extends SimpleHandler<String>{
    @Override
    void onRemoved(HandlerContext ctx) {

    }

    @Override
    void onAdded(HandlerContext ctx) {
        ctx.enableRead();
        ctx.enableWrite();
    }

    @Override
    void channelRead0(HandlerContext ctx, String msg) {
        System.out.println(msg);
    }

    @Override
    void channelWrite0(HandlerContext ctx, String msg) {
        System.out.println(msg);
    }
}
