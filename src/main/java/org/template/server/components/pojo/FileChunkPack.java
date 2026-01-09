package org.template.server.components.pojo;

import java.nio.ByteBuffer;

public class FileChunkPack extends BufferPack{

    private String uuid;

    private int chunkId;

    private long dataLen;

    private String sha256;

    public FileChunkPack(ByteBuffer data) {
        super(Upload_Chunk_SYN, data);
    }

    public FileChunkPack(ByteBuffer data,String uuid,String sha256,int chunkId,long dataLen){
        this(data);
        this.dataLen =dataLen;
        this.chunkId = chunkId;
        this.uuid = uuid;
        this.sha256 = sha256;
    }

    public String getSha256() {
        return sha256;
    }

    public String getUuid() {
        return uuid;
    }

    public long getDataLen() {
        return dataLen;
    }

    public int getChunkId() {return chunkId;}


}
