package org.template.server.components.pojo;

import org.template.server.components.abstracts.ToByte;

import java.nio.ByteBuffer;

public class UploadAckBufferPack extends BufferPack{

    @ToByte
    private String uuid;

    public UploadAckBufferPack(String uuid) {
        super(Upload_ACK, null);
        this.uuid = uuid;
        try {
            setData(toByteBuffer(true));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
