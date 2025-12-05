package org.template.server.components;

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class BasePipeline {
    private final HashMap<String,HandlerContext> ctxMap = new HashMap<>();

    private SocketChannel socketChannel;
    private HandlerContext head;

    private HandlerContext tail;

    private Logger logger;

    private int capacity = 64;

    private boolean direct = false;

    public BasePipeline(){
    }

    public void setLogger(Logger logger){
        this.logger = logger;
    }

    public void setSocketChannel(SocketChannel channel){
        this.socketChannel = channel;
    }

    public SocketChannel getSocketChannel(){
        return socketChannel;
    }

    public BasePipeline addLast(String id,SimpleHandler<?> handler){
        HandlerContext newCtx = new HandlerContext(this,id,handler);
        if (ctxMap.containsKey(id)){ //replace ctx if existed
            replaceContext(id, newCtx);
        }else{
            newCtx.setPre(tail);
            if (tail!=null)
                tail.setNext(newCtx);
            if (head == null)
                head = newCtx;
            this.tail = newCtx;
        }
        this.ctxMap.put(id,newCtx);
        handler.onAdded(newCtx);
        return this;
    }

    private void replaceContext(String id, HandlerContext newCtx) {
        HandlerContext oldCtx = ctxMap.get(id);
        newCtx.setPre(oldCtx.getPre());
        newCtx.setNext(oldCtx.getNext());
        if (head.equals(oldCtx)){
            this.head = newCtx;
        }
        if (tail.equals(oldCtx)){
            this.tail = newCtx;
        }
        if (oldCtx.getPre()!=null)
            oldCtx.getPre().setNext(newCtx);
        if (oldCtx.getNext()!=null)
            oldCtx.getNext().setPre(newCtx);
    }

    public BasePipeline addHead(String id,SimpleHandler<?> handler){
        HandlerContext newCtx = new HandlerContext(this,id,handler);
        if (ctxMap.containsKey(id)){ //replace ctx if existed
            replaceContext(id, newCtx);
        }else{
            newCtx.setNext(head);
            if (head!=null)
                head.setPre(newCtx);
            if (tail == null)
                tail = newCtx;
            this.head = newCtx;
        }
        this.ctxMap.put(id,newCtx);
        return this;
    }

    public HandlerContext removeContext(String id){
        HandlerContext reCtx = null;
        if (ctxMap.containsKey(id)){
            HandlerContext ctx = ctxMap.get(id);
            if (ctx.getPre()!=null){
                ctx.getPre().setNext(ctx.getNext());
            }
            if (ctx.getNext()!=null){
                ctx.getNext().setPre(ctx.getPre());
            }
            if (ctx.equals(head)){
                head = ctx.getNext();
            }
            if (ctx.equals(tail)) {
                tail = ctx.getPre();
            }
            reCtx = ctxMap.remove(id);
        }
        return reCtx;
    }

    public HandlerContext getContext(String id){
        return ctxMap.get(id);
    }

        public BasePipeline addBefore(String tarId, String id, SimpleHandler<?> newHandler) {
        HandlerContext targetCtx = ctxMap.get(tarId);
        HandlerContext newCtx = new HandlerContext(this,id,newHandler);
        if (targetCtx == null) {
            throw new IllegalArgumentException("Target handler not found: " + tarId);
        }
        HandlerContext prevCtx = targetCtx.getPre();
        newCtx.setNext(targetCtx);
        if (prevCtx != null) {
            prevCtx.setNext(newCtx);
        }
        if (targetCtx.equals(head)) {
            head = newCtx;
        }
        targetCtx.setPre(newCtx);
        ctxMap.put(id, newCtx);
        return this;
    }


    public BasePipeline addAfter(String tarId, String id,SimpleHandler<?> newHandler) {
        HandlerContext newCtx = new HandlerContext(this,id,newHandler);
        HandlerContext targetCtx = ctxMap.get(tarId);
        if (targetCtx == null) {
            throw new IllegalArgumentException("Target handler not found: " + tarId);
        }
        HandlerContext nextCtx = targetCtx.getNext();
        newCtx.setPre(targetCtx);
        newCtx.setNext(nextCtx);
        if (nextCtx != null) {
            nextCtx.setPre(newCtx);
        }
        if (targetCtx.equals(tail)) {
            tail = newCtx;
        }
        targetCtx.setNext(newCtx);
        ctxMap.put(id, newCtx);
        return this;
    }

    public void fireHandlersFromBegin(SocketChannel channel,ByteBuffer buffer){
        this.socketChannel = channel;
        this.head.fireReadHandler(buffer);
    }

    public void setCapacity(int capacity){
        this.capacity = capacity;
    }

    public ByteBuffer allocateBuffer(){
        if (direct)
            return ByteBuffer.allocateDirect(this.capacity);
        return ByteBuffer.allocate(this.capacity);
    }

    public void enableDirectMemory(){
        this.direct = true;
    }

    public void disableDirectMemory(){
        this.direct = false;
    }

    public Logger getLogger() {
        return logger;
    }
}
