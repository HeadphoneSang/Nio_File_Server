package org.template.server.components.pojo;

import org.template.server.components.abstracts.ToByte;

import java.nio.ByteBuffer;

public class UploadCompletePack extends BufferPack{

    @ToByte
    private String uuid;

    public UploadCompletePack(FileChunkPack fileChunk) {
        super(Upload_Complete, null);
        this.uuid = fileChunk.getUuid();
        try {
            setData(toByteBuffer(true));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
