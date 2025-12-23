package org.template.server.components.pojo;

import org.template.server.components.abstracts.ToByte;

import java.nio.ByteBuffer;

public class StringBufferPack extends BufferPack{

    @ToByte
    private String sha256;

    @ToByte
    private String fileName;

    @ToByte
    private long ackLen;

    public StringBufferPack(int opCode, ByteBuffer data) {
        super(opCode, data);
    }
    public StringBufferPack(int opCode,String sha256,String fileName,long ackLen){
        this(opCode,null);
        this.sha256 = sha256;
        this.ackLen = ackLen;
        this.fileName = fileName;
        try {
            ByteBuffer buffer = toByteBuffer(true);
            this.setData(buffer);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
