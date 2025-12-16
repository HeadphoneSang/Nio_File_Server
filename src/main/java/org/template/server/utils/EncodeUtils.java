package org.template.server.utils;

public class EncodeUtils {
    private EncodeUtils(){}

    public static byte[] encodeToBigBytes(int x,int codeLen){
        byte[] ans = new byte[codeLen];
        ans[0] = (byte)(x >>> 3*8);
        ans[1] = (byte)(x >>> 2*8);
        ans[2] = (byte)(x >>> 8);
        ans[3] = (byte)x;
        return ans;
    }
}
