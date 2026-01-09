package org.template.server.components.handlers.subHandlers;

import org.template.server.components.handlers.FileUploadHandler;
import org.template.server.components.internals.HandlerContext;
import org.template.server.components.pojo.BufferPack;
import org.template.server.components.internals.pojo.FileReceiveContext;
import org.template.server.components.pojo.UploadAckBufferPack;
import org.template.server.components.pojo.UploadFailBufferPack;
import org.template.server.utils.DecodeUtils;

public class UploadFailHandler implements SimpleSubHandler<BufferPack> {
    @Override
    public void handler(HandlerContext ctx, BufferPack msg) {
        FileReceiveContext fileReceiveContext = DecodeUtils.decodeFileInfo(msg.getData());
        FileUploadHandler fileUploadHandler = (FileUploadHandler) ctx.getPipeline().getContext("uploadHandler").getHandler();
        ctx.executeTask((c)->{
            boolean remSUC = fileUploadHandler.remTmpFile(fileReceiveContext);
            if (remSUC){
                fileUploadHandler.registerFileUpload(fileReceiveContext);
                BufferPack ackPack = new UploadAckBufferPack(fileReceiveContext.getUuid());
                ctx.writeAndFlush(ackPack).addListener((future -> {
                    if (future.isSuccess()){
                        System.out.println("删除冲突文件-成功-建立上传");
                    }
                }));
            }else{
                BufferPack ackPack = new UploadFailBufferPack(fileReceiveContext.getUuid());
                ctx.writeAndFlush(ackPack).addListener((future -> {
                    if (future.isSuccess()){
                        System.out.println("删除冲突文件-失败");
                    }
                }));
            }
        });
    }
}
