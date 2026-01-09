package org.template.server.components.handlers.subHandlers;

import org.template.server.components.internals.HandlerContext;
import org.template.server.components.pojo.BufferPack;
import org.template.server.components.pojo.FileChunkPack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 确认Upload_Chunk_SYN
 */
public class UploadChunkHandler implements SimpleSubHandler<BufferPack>{
    @Override
    public void handler(HandlerContext ctx, BufferPack msg) {
        ByteBuffer meta = msg.getData();
        long dataLen = meta.getLong();
        int chunkId = meta.getInt();
        int uuidLen = meta.getInt();
        byte[] bytes = new byte[uuidLen];
        meta.get(bytes,0,uuidLen);
        String uuid = new String(bytes, StandardCharsets.UTF_8);
        int sha256Len = meta.getInt();
        bytes = new byte[sha256Len];
        meta.get(bytes,0,sha256Len);
        String rSha256 = new String(bytes,StandardCharsets.UTF_8);
        ByteBuffer chunkBytes = meta.slice().asReadOnlyBuffer();
        //打包成块对象发给下一个处理器
        FileChunkPack pack = new FileChunkPack(chunkBytes,uuid,rSha256,chunkId,dataLen);
        ctx.fireNextReadHandler(pack);
    }
}
