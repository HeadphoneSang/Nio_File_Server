package org.template.server.components.handlers.subHandlers;

import org.template.server.components.handlers.DirectBufferDecodeHandler;
import org.template.server.components.handlers.FileUploadHandler;
import org.template.server.components.internals.HandlerContext;
import org.template.server.components.internals.pojo.FileReceiveContext;
import org.template.server.components.pojo.*;
import org.template.server.utils.DecodeUtils;
import org.template.server.utils.EncodeUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class EstablishUploadHandler implements SimpleSubHandler<BufferPack>{
    @Override
    public void handler(HandlerContext ctx, BufferPack msg) {
    /**
    * 建立一个文件上传的管道，先把文件上传管道放在决策管道后面，然后讲传输buffer改成直接内存，然后把管道的缓存内存也改成直接内存
    * 把前面处理半包粘包的处理器换成处理直接内存的，然后将当前文件上传协议封装成UploadPack注册进文件上传处理器，这样就建立了一个上传管道
    * 如果已经建立了，只需要取出文件上传处理器然后注册新的文件
    */
        if (ctx.getPipeline().getContext("uploadHandler")==null){
            ctx.getPipeline().addAfter(ctx.getId(),"uploadHandler",new FileUploadHandler());
            ctx.getPipeline().removeContext("simpleHandler");
            ctx.getPipeline().addBefore("decision","directHandler",new DirectBufferDecodeHandler(5*1024*1024));
            ctx.getPipeline().enableDirectMemory();
        }
        FileUploadHandler fileUploadHandler = (FileUploadHandler) ctx.getPipeline().getContext("uploadHandler").getHandler();
        FileReceiveContext fileReceiveContext = DecodeUtils.decodeFileInfo(msg.getData());
        boolean successReg = fileUploadHandler.registerFileUpload(fileReceiveContext);
        if (!successReg){
            if(fileReceiveContext.getFileSize()<0){
                /**
                 * 处理续传
                 */
//                ctx.executeTask(ctx1 -> {
//                    Path tmpPath = new File(fileUploadHandler.getSaveDir(), fileReceiveContext.getFileName()).toPath();
//                    try {
//                        String sha256 = EncodeUtils.sha256(tmpPath);
//                        ctx1.getLogger().info("重传校验，编码服务器半包文件: "+sha256);
//                        StringBufferPack pack = new StringBufferPack(Datapack.ReUpload_ACK,sha256, fileReceiveContext.getUuid(), fileReceiveContext.getFileSize()*-1);
//                        ctx1.writeAndFlush(pack);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                })
                FileReceiveContext oldState = FileReceiveContext.loadFromUUID(fileReceiveContext.getUuid());
                assert oldState!=null;

                BufferPack rePack = null;
                if (fileReceiveContext.copyStateFrom(oldState)){
                    oldState.remPersist();
                    fileUploadHandler.registerFileUpload(fileReceiveContext);
                    //构建帧
                    byte[] bitMap = oldState.getBitMap();           // 获取位图数据（表示已接收的数据块状态）
                    byte[] uuid = oldState.getUuid().getBytes(StandardCharsets.UTF_8);  // 将UUID转换为UTF-8字节数组
                    int uuidLen = uuid.length;                      // UUID字节数组长度
                    int bitMapLen = bitMap.length;                  // 位图字节数组长度
                    ByteBuffer buffer = ByteBuffer.allocateDirect(8+uuidLen+bitMapLen);  // 分配直接内存缓冲区，总大小=4字节UUID长度+UUID数据+4字节位图长度+位图数据
                    buffer.putInt(uuidLen);                         // 写入UUID长度（4字节整数）
                    buffer.put(uuid);                               // 写入UUID数据
                    buffer.putInt(bitMapLen);                       // 写入位图长度（4字节整数）
                    buffer.put(bitMap);                             // 写入位图数据
                    buffer.flip();
                    rePack = new BufferPack(Datapack.ReUpload_ACK,buffer);  // 创建重传确认包
                }
                else{
                    rePack = new UploadFailBufferPack(oldState.getUuid());
                }
                ctx.writeAndFlush(rePack);
            }else{
                BufferPack ackPack = new UploadFailBufferPack(fileReceiveContext.getUuid());
                ctx.writeAndFlush(ackPack).addListener((future -> {
                    if (future.isSuccess()){
                        System.out.println("文件: "+fileReceiveContext.getFileName()+" 已存在或正在上传，上传建立失败");
                    }
                }));
            }
        }else{
            BufferPack ackPack = new UploadAckBufferPack(fileReceiveContext.getUuid());
            ctx.writeAndFlush(ackPack).addListener((future -> {
                if (future.isSuccess()){
                    System.out.println("发送成功");
                }
            }));
        }
    }
}
