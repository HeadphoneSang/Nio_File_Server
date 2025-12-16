package org.template.server.components.handlers;

import org.template.server.components.HandlerContext;
import org.template.server.components.WritePromise;
import org.template.server.components.pojo.BufferPack;
import org.template.server.components.pojo.FileInfo;
import org.template.server.components.pojo.ObjPack;
import org.template.server.components.pojo.Datapack;
import org.template.server.utils.DecodeUtils;

public class DecisionHandler extends SimpleHandler<ObjPack,ObjPack>{

    @Override
    public void onAdded(HandlerContext ctx) {
        ctx.enableRead();
        ctx.disableWrite();
    }

    @Override
    public void write(HandlerContext ctx, WritePromise promise, ObjPack msg) {

    }

    @Override
    public void onRemoved(HandlerContext ctx) {
        ctx.getLogger().info("removed");
    }

    @Override
    public void onDestroy(HandlerContext ctx) {
        ctx.getLogger().info("destroyed");
    }

    @Override
    public void channelRead0(HandlerContext ctx, ObjPack msg) {
        switch(msg.getOperator()){
            case Datapack.Upload:{
                /**
                 * 建立一个文件上传的管道，先把文件上传管道放在决策管道后面，然后讲传输buffer改成直接内存，然后把管道的缓存内存也改成直接内存
                 * 把前面处理半包粘包的处理器换成处理直接内存的，然后将当前文件上传协议封装成UploadPack注册进文件上传处理器，这样就建立了一个上传管道
                 * 如果已经建立了，只需要取出文件上传处理器然后注册新的文件
                 */
                if (ctx.getPipeline().getContext("uploadHandler")==null){
                    ctx.getPipeline().enableDirectMemory();
                    ctx.getPipeline().addAfter(ctx.getId(),"uploadHandler",new FileUploadHandler());
                    ctx.getPipeline().addAfter("simpleHandler","directHandler",new DirectBufferDecodeHandler(5*1024*1024));
                    ctx.getPipeline().removeContext("simpleHandler");
                }
                FileUploadHandler fileUploadHandler = (FileUploadHandler) ctx.getPipeline().getContext("uploadHandler").getHandler();
                FileInfo fileInfo = DecodeUtils.decodeFileInfo((byte[])msg.getData());
                fileUploadHandler.registerFileUpload(fileInfo);
                BufferPack ackPack = new BufferPack(Datapack.Upload_ACK,null);
                ctx.write(ackPack).addListener((future -> {
                    if (future.isSuccess()){
                        System.out.println("发送成功");
                    }
                }));
                ctx.flush();
                break;
            }
            case Datapack.Upload_Chunk:{
                break;
            }
            case Datapack.Download:{
                break;
            }
            case Datapack.String_Send:{
                break;
            }
        }
    }
}
