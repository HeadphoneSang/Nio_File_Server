package org.template.server.components.pojo;

import org.template.server.components.abstracts.ToByte;

import java.nio.ByteBuffer;

public class UploadChunkAckPack extends BufferPack{
    @ToByte
    private final int chunkId;

    @ToByte
    private final String uuid;




    public UploadChunkAckPack(FileChunkPack chunkPack) {
        super(Datapack.Upload_Chunk_ACK,null);
        this.chunkId = chunkPack.getChunkId();
        this.uuid = chunkPack.getUuid();
        try {
            setData(toByteBuffer(true));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
