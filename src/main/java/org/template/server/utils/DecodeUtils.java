package org.template.server.utils;

import org.template.server.components.internals.pojo.FileReceiveContext;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DecodeUtils {

    private DecodeUtils(){}

    public static int readBEInt(byte[] b4){
        return ((b4[0] & 0xff) << 3*8) | ((b4[1] & 0xff) << 2*8) | ((b4[2] & 0xff) << 8) | ((b4[3] & 0xff));
    }

    public static long readBELong(byte[] b8){
        return ((long) (b8[0] & 0xff) << 7*8) |
                ((long) (b8[1] & 0xff) << 6*8) |
                ((long) (b8[2] & 0xff) << 5*8) |
                ((long) (b8[3] & 0xff) << 4*8) |
                ((long) (b8[4] & 0xff) << 3*8) |
                ((long) (b8[5] & 0xff) << 2*8) |
                ((long) (b8[6] & 0xff) << 8) |
                ((long) (b8[7] & 0xff));
    }

    public static FileReceiveContext decodeFileInfo(ByteBuffer data){
        long fileSize = data.getLong();
        int chunkSize = data.getInt();
        int totalChunks =data.getInt();
        int uuidLen = data.getInt();
        int fileNameLen = data.getInt();
        byte[] uuidB = new byte[uuidLen];
        data.get(uuidB,0,uuidLen);
        byte[] fileNameB = new byte[fileNameLen];
        data.get(fileNameB,0,fileNameLen);
        String fileName = new String(fileNameB, StandardCharsets.UTF_8);
        String uuid = new String(uuidB,StandardCharsets.UTF_8);
        return new FileReceiveContext(fileSize,chunkSize,totalChunks,uuid,fileName);
    }
}
