package org.template.server.components;

import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerContext {
    private HandlerContext pre;

    private HandlerContext next;

    private final SimpleHandler<?> handler;

    private final BasePipeline pipeline;

    private String id;

    private boolean isRead;

    private boolean isWrite;

    public HandlerContext(BasePipeline pipe,String id,SimpleHandler<?> handler,HandlerContext pre,HandlerContext next,boolean isRead,boolean isWrite){
        this.id = id;
        this.pipeline = pipe;
        this.handler = handler;
        this.next = next;
        this.pre = pre;
        this.isRead = isRead;
        this.isWrite = isWrite;
    }

    public HandlerContext(BasePipeline pipeline,String id,SimpleHandler<?> handler,HandlerContext pre,HandlerContext next){
        this(pipeline,id,handler,pre,next,false,false);
    }

    public HandlerContext(BasePipeline pipeline,String id,SimpleHandler<?> handler){
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
        while(!nextCtx.isRead)
            nextCtx = nextCtx.next;
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

    public void fireWriteHandler(Object msg){
        if (isWrite) {
            boolean valid = this.handler.channelWrite(this, msg);
            if (!valid){
                System.out.println("Handler "+ this.getId() +" is not writable!");
                fireNextWriteHandler(msg);
            }
        }else
            fireNextWriteHandler(msg);
    }

    public void fireNextWriteHandler(Object msg){
        HandlerContext nextCtx = next;
        if (nextCtx == null)
            return;
        while(!nextCtx.isWrite)
            nextCtx = nextCtx.next;
        nextCtx.fireWriteHandler(msg);
    }

    public BasePipeline getPipeline() {
        return pipeline;
    }

    public Logger getLogger(){
        return this.pipeline.getLogger();
    }

    public SimpleHandler<?> getHandler(){
        return this.handler;
    }
}
