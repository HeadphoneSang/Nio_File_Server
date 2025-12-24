package org.template.server.components.internals;

import org.template.server.components.abstracts.InitPipeline;
import org.template.server.components.handlers.DefaultHandler;
import org.template.server.components.handlers.SimpleHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerBootStrap {

    private AbstractEventLoopGroup bossGroup;

    private AbstractEventLoopGroup workerGroup;

    private ServerSocketChannel serverChannel;

    public ServerBootStrap group(AbstractEventLoopGroup bossLoop,AbstractEventLoopGroup workerLoop){
        this.bossGroup = bossLoop;
        this.workerGroup = workerLoop;
        return this;
    }

    public ServerBootStrap localAddress(InetSocketAddress address){
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(address);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public ServerBootStrap childHandlers(InitPipeline initPipeline){
        workerGroup.setInitPipeline(()->{
            BasePipeline pipeline = initPipeline.init();
            DefaultHandler defaultHandler = new DefaultHandler();
            pipeline.addHead("DefHd",defaultHandler);
            return pipeline;
        });
        return this;
    }

    public ChannelPromise bind(){
        bossGroup.setInitPipeline(()->{
            BasePipeline pipeline = new BasePipeline(false,0);
            pipeline.addLast("acceptor",new ServerAcceptorHandler(workerGroup));
            return pipeline;
        });
        return bossGroup.register(this.serverChannel);
    }

    static class ServerAcceptorHandler extends SimpleHandler<SocketChannel,Object>{
        private final AbstractEventLoopGroup workerGroup;

        ServerAcceptorHandler(AbstractEventLoopGroup workerGroup) {
            this.workerGroup = workerGroup;
        }

        @Override
        public void write(HandlerContext ctx, WritePromise promise, Object SocketChannel) {

        }

        @Override
        public void onRemoved(HandlerContext ctx) {

        }

        @Override
        public void onAdded(HandlerContext ctx) {
            ctx.enableRead();
            ctx.disableWrite();
        }

        @Override
        public void onDestroy(HandlerContext ctx) {

        }

        @Override
        public void channelRead0(HandlerContext ctx, SocketChannel client) {
            workerGroup.register(client).addListener((future -> {
                /**
                 * Todo 处理将任务是否成功分发到工作循环的逻辑
                 */
                if (future.isNotSuccess()){

                }
            }));
        }
    }

}
