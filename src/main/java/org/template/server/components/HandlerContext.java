package org.template.server.components;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.template.server.components.handlers.SimpleHandler;

public class HandlerContext {
    private HandlerContext pre;

    private HandlerContext next;

    private final SimpleHandler<?,?> handler;

    private final BasePipeline pipeline;

    private String id;

    private boolean isRead;

    private boolean isWrite;

    public HandlerContext(BasePipeline pipe,String id,SimpleHandler<?,?> handler,HandlerContext pre,HandlerContext next,boolean isRead,boolean isWrite){
        this.id = id;
        this.pipeline = pipe;
        this.handler = handler;
        this.next = next;
        this.pre = pre;
        this.isRead = isRead;
        this.isWrite = isWrite;
    }

    public HandlerContext(BasePipeline pipeline,String id,SimpleHandler<?,?> handler,HandlerContext pre,HandlerContext next){
        this(pipeline,id,handler,pre,next,false,false);
    }

    public HandlerContext(BasePipeline pipeline,String id,SimpleHandler<?,?> handler){
        this(pipeline,id,handler,null,null,false,false);
    }

    public void enableRead(){
        this.isRead = true;
    }

    public void enableWrite(){
        this.isWrite = true;
    }

    public void disableRead(){
        this.isRead = false;
    }

    public void disableWrite(){
        this.isWrite = false;
    }

    public HandlerContext getPre() {
        return pre;
    }

    public HandlerContext getNext() {
        return next;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isWrite() {
        return isWrite;
    }

    public String getId(){return id;}

    public void setPre(HandlerContext pre) {
        this.pre = pre;
    }

    public void setNext(HandlerContext next) {
        this.next = next;
    }


    public SocketChannel getSocketChannel(){
        return pipeline.getSocketChannel();
    }

    public void fireNextReadHandler(Object msg){
        HandlerContext nextCtx = next;
        if (nextCtx == null)
            return;
        while(nextCtx!=null&&!nextCtx.isRead)
            nextCtx = nextCtx.next;
        if (nextCtx==null)
            return;
        nextCtx.fireReadHandler(msg);
    }

    public void fireReadHandler(Object msg){
        if (isRead) {
            boolean valid = this.handler.channelRead(this, msg);
            if (!valid){
                System.out.println("Handler"+ this.getId() +" is not readable");
                fireNextReadHandler(msg);
            }
        }else
            fireNextReadHandler(msg);
    }

    public void fireWriteHandler(Object msg,WritePromise promise){
        if (isWrite) {
            boolean valid = this.handler.channelWrite(this, promise,msg);
            if (!valid){
                System.out.println("Handler "+ this.getId() +" is not writable!");
                fireNextWriteHandler(msg,promise);
            }
        }else
            fireNextWriteHandler(msg,promise);
    }

    public void fireNextWriteHandler(Object msg,WritePromise promise){
        HandlerContext preCtx = pre;
        if (preCtx == null)
            return;
        while(!preCtx.isWrite)
            preCtx = preCtx.pre;
        preCtx.fireWriteHandler(msg,promise);
    }

    public BasePipeline getPipeline() {
        return pipeline;
    }

    public Logger getLogger(){
        return this.pipeline.getLogger();
    }

    public SimpleHandler<?,?> getHandler(){
        return this.handler;
    }

    public void flush(){
        this.pipeline.flush();
    }


    public WritePromise write(Object msg){
        HandlerContext nowCtx = this;
        WritePromise promise = new WritePromise();
        if (!nowCtx.isWrite){
            HandlerContext preCtx = pre;
            while(!preCtx.isWrite)
                preCtx = preCtx.pre;
            preCtx.fireWriteHandler(msg,promise);
        }else{
            fireWriteHandler(msg,promise);
        }
        return promise;
    }


    /**
     * 尝试往缓冲区写数据，写不进去就先发送一次缓冲区，然后再写
     * 如果os缓冲区写不进去，多次尝试后，返回false。
     * @param buffer 要写进缓冲区的buffer
     * @return 是否成功写进缓冲区
     */
//    public boolean write(ByteBuffer buffer){
//        boolean ans = false;
//        int stack = 0;
//        ByteBuffer outBuffer = this.pipeline.getOutBuffer();
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
//                int wroteLen = this.pipeline.flushAllBuffer();
//                if (wroteLen<writeLen){//os缓冲区写不进去所有的内存缓冲区的数据
//                    outBuffer.compact();
//                    stack++;
//                }else{//旧的缓冲区全部写出
//                    outBuffer.clear();
//                    //将剩余的写入新的缓冲区
//                }
//            }
//        }
//        return ans;
//    }


}
