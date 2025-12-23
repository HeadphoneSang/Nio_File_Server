package org.template.server.components.handlers;

import org.template.server.components.HandlerContext;
import org.template.server.components.WritePromise;
import org.template.server.components.pojo.*;
import org.template.server.utils.DecodeUtils;
import org.template.server.utils.EncodeUtils;

import java.io.File;
import java.nio.file.Path;

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
                boolean successReg = fileUploadHandler.registerFileUpload(fileInfo);
                if (!successReg){
                    if(fileInfo.getFileSize()<0){
                        /**
                         * 处理续传
                         */
                        ctx.executeTask(ctx1 -> {
                            Path tmpPath = new File(fileUploadHandler.getSaveDir(),fileInfo.getFileName()).toPath();
                            try {
                                String sha256 = EncodeUtils.sha256(tmpPath);
                                ctx1.getLogger().info("重传校验，编码服务器半包文件: "+sha256);
                                StringBufferPack pack = new StringBufferPack(Datapack.ReUpload_ACK,sha256,fileInfo.getFileName(),fileInfo.getFileSize()*-1);
                                ctx1.write(pack);
                                ctx1.flush();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }else{
                        /**
                         * 上传失败
                          */
                    }
                }else{
                    BufferPack ackPack = new BufferPack(Datapack.Upload_ACK,null);
                    ctx.write(ackPack).addListener((future -> {
                        if (future.isSuccess()){
                            System.out.println("发送成功");
                        }
                    }));
                    ctx.flush();
                }
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
