package org.template.server.utils;

import org.template.server.components.pojo.FileInfo;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    public static FileInfo decodeFileInfo(byte[] data){
        long fileSize = readBELong(Arrays.copyOfRange(data,0,8));
        int fileChunks = readBEInt(Arrays.copyOfRange(data,8,12));
        int uuidLen = readBEInt(Arrays.copyOfRange(data,12,16));
        int fileNameLen = readBEInt(Arrays.copyOfRange(data,16,20));
        int begin = 20;
        byte[] uuidB = Arrays.copyOfRange(data,begin,begin+uuidLen);
        begin += uuidLen;
        byte[] fileNameB = Arrays.copyOfRange(data,begin,begin+fileNameLen);
        String fileName = new String(fileNameB, StandardCharsets.UTF_8);
        String uuid = new String(uuidB,StandardCharsets.UTF_8);
        return new FileInfo(fileSize,fileChunks,uuid,fileName);
    }
}
