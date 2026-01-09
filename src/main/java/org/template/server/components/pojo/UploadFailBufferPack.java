package org.template.server.components.pojo;

import org.template.server.components.abstracts.ToByte;

import java.nio.ByteBuffer;

public class UploadFailBufferPack extends BufferPack{

    @ToByte
    private String uuid;

    public UploadFailBufferPack(String uuid) {
        super(UPLOAD_FAIL, null);
        this.uuid = uuid;
        try {
            setData(toByteBuffer(true));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
