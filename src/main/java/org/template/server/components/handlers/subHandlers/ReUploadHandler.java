package org.template.server.components.handlers.subHandlers;

import org.template.server.components.handlers.FileUploadHandler;
import org.template.server.components.internals.HandlerContext;
import org.template.server.components.pojo.BufferPack;
import org.template.server.components.internals.pojo.FileReceiveContext;
import org.template.server.components.pojo.UploadAckBufferPack;
import org.template.server.utils.DecodeUtils;

public class ReUploadHandler implements SimpleSubHandler<BufferPack> {
    @Override
    public void handler(HandlerContext ctx, BufferPack msg) {
        FileReceiveContext fileReceiveContext = DecodeUtils.decodeFileInfo(msg.getData());
        FileUploadHandler fileUploadHandler = (FileUploadHandler) ctx.getPipeline().getContext("uploadHandler").getHandler();
        fileUploadHandler.editFileUpload(fileReceiveContext);
        BufferPack ackPack = new UploadAckBufferPack(fileReceiveContext.getUuid());
        ctx.writeAndFlush(ackPack).addListener((future -> {
            if (future.isSuccess()){
                System.out.println("续传通道建立-发送成功");
            }
        }));
    }
}
