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

    /**
     * 处理上传数据块请求
     * <p>
     * 数据帧结构：
     * - int: 块ID (4字节)
     * - int: UUID长度 (4字节)
     * - byte[]: UUID字符串内容 (可变长度，根据UUID长度确定)
     * - int: SHA256哈希值长度 (4字节)
     * - byte[]: SHA256哈希值内容 (可变长度，根据SHA256长度确定)
     * - ByteBuffer: 实际数据块内容 (剩余所有字节)
     * <p>
     * 从BufferPack消息中解析出数据块的所有元信息，并将数据块封装成FileChunkPack对象传递给下一个处理器
     */
    @Override
    public void handler(HandlerContext ctx, BufferPack msg) {
        ByteBuffer meta = msg.getData();
//        long dataLen = meta.getLong();  //感觉这个可以不加，毕竟先读定长数据和短的不定长数据，剩下的都是实际数据
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
        FileChunkPack pack = new FileChunkPack(chunkBytes,uuid,rSha256,chunkId,chunkBytes.remaining());
        ctx.fireNextReadHandler(pack);
    }


}
