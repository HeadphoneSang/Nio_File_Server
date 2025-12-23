package org.template.server.utils;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

public class EncodeUtils {
    private EncodeUtils(){}

    public static byte[] encodeToBigBytes(int x,int codeLen){
        byte[] ans = new byte[4];
        ans[0] = (byte)(x >>> 3*8);
        ans[1] = (byte)(x >>> 2*8);
        ans[2] = (byte)(x >>> 8);
        ans[3] = (byte)x;
        if (codeLen>4)
        {
            byte[] ori = ans;
            ans = new byte[codeLen];
            System.arraycopy(ori,0,ans,codeLen-ori.length,ori.length);
        }
        return ans;
    }
    public static byte[] encodeToBigBytes(long x){
        byte[] ans = new byte[8];
        ans[0] = (byte)(x >>> 7*8);
        ans[1] = (byte)(x >>> 6*8);
        ans[2] = (byte)(x >>> 5*8);
        ans[3] = (byte)(x >>> 4*8);
        ans[4] = (byte)(x >>> 3*8);
        ans[5] = (byte)(x >>> 2*8);
        ans[6] = (byte)(x >>> 8);
        ans[7] = (byte)(x);
        return ans;
    }

    public static String sha256(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1 << 20); // 1MB
            while (channel.read(buffer) > 0) {
                buffer.flip();
                md.update(buffer);
                buffer.clear();
            }
        }

        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
