package org.template.server.components.pojo;

import org.template.server.components.abstracts.ToByte;

public class UploadChunkFinPack extends BufferPack{
    @ToByte
    private final int chunkId;

    @ToByte
    private int finCode;

    @ToByte
    private final String uuid;


    public UploadChunkFinPack(FileChunkPack chunkPack,FinCode finCode) {
        super(Datapack.Upload_Chunk_FIN,null);
        this.chunkId = chunkPack.getChunkId();
        this.uuid = chunkPack.getUuid();
        this.finCode = finCode.getCode();
        try {
            setData(toByteBuffer(true));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public enum FinCode{
        CHUNK_INVALID(0),
        CHUNK_EXISTED(1);

        private final int code;

        FinCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
