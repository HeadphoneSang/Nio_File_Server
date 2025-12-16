package org.template.test;

import java.nio.ByteBuffer;

public class writeTest {

    private ByteBuffer outBuffer = ByteBuffer.allocate(32);

    private ByteBuffer os_Heap = ByteBuffer.allocate(50);



    public int flushAllBuffer(){
        outBuffer.flip();
        int os_remain = os_Heap.remaining();
        int outBuffer_total = outBuffer.remaining();
        int real_read_len = Math.min(os_remain,outBuffer_total);
        byte[] read_cache = new byte[real_read_len];
        outBuffer.get(read_cache,0,real_read_len);
        os_Heap.put(read_cache);
        return real_read_len;
    }

//    public boolean write0(ByteBuffer buffer, int stackDeep){
//        if (stackDeep>10){
//            return false;
//        }
////        outBuffer.compact();写模式不能切换到写模式
//        if (outBuffer.remaining()>=buffer.remaining()){//还能写进去
//            outBuffer.put(buffer);
//            return true;
//        }else{//写不进缓冲区
//            int availableLen = outBuffer.remaining();
//            ByteBuffer subBuffer = buffer.slice();
//            subBuffer.limit(availableLen);
//            outBuffer.put(subBuffer);//将写出缓存写满
//            buffer.position(buffer.position() + availableLen);
//            int writeLen = outBuffer.position();
//            int wroteLen = flushAllBuffer();
//            if (wroteLen<writeLen){//os缓冲区写不进去所有的内存缓冲区的数据
//                outBuffer.compact();
//                return this.write0(buffer,stackDeep+1);
//            }else{//旧的缓冲区全部写出
//                outBuffer.clear();
//                return this.write0(buffer,stackDeep);//将剩余的写入新的缓冲区
//            }
//        }
//    }

//    public boolean write(ByteBuffer buffer){
//        boolean ans = false;
//        int stack = 0;
//        while (stack < 10){
//            if (outBuffer.remaining()>=buffer.remaining()){//还能写进去
//                outBuffer.put(buffer);
//                ans = true;
//                break;
//            }else{
//                //写不进缓冲区
//                int availableLen = outBuffer.remaining();
//                ByteBuffer subBuffer = buffer.slice();
//                subBuffer.limit(availableLen);
//                outBuffer.put(subBuffer);//将写出缓存写满
//                buffer.position(buffer.position() + availableLen);
//                int writeLen = outBuffer.position();
//                int wroteLen = flushAllBuffer();
//                if (wroteLen<writeLen){//os缓冲区写不进去所有的内存缓冲区的数据
//                    outBuffer.compact();
//                    stack++;
//                }else{//旧的缓冲区全部写出
//                    outBuffer.clear();
//                   //将剩余的写入新的缓冲区
//                }
//            }
//        }
//        return ans;
//    }

//    public static void main(String[] args) {
//        writeTest test = new writeTest();
//        byte[] putBytes = new byte[20];
//        for(int i = 0;i<20;i++){
//            putBytes[i] = (byte) i;
//        }
//        ByteBuffer putBuffer = ByteBuffer.wrap(putBytes);
//        System.out.println(test.write(putBuffer));
//        putBuffer.clear();
//        System.out.println(test.write(putBuffer));;
//        putBuffer.clear();
//        System.out.println(test.write(putBuffer));;
//        putBuffer.clear();
//        System.out.println(test.write(putBuffer));;
//        putBuffer.clear();
//        System.out.println(test.write(putBuffer));
//        System.out.println(putBuffer.remaining());
//    }

}
