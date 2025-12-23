package org.template.server.components;

import org.slf4j.Logger;
import org.template.server.components.abstracts.Executor;
import org.template.server.components.handlers.DefaultHandler;
import org.template.server.components.handlers.SimpleHandler;
import org.template.server.components.pojo.PromiseEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class BasePipeline {
    private final HashMap<String,HandlerContext> ctxMap = new HashMap<>();

    private final ArrayDeque<PromiseEntry<ByteBuffer>> writeQueue = new ArrayDeque<>();

    private final ArrayDeque<Map.Entry<HandlerContext,Executor>> executorQueue = new ArrayDeque<>();

    private PromiseEntry<ByteBuffer> flushTail;

    private SocketChannel socketChannel;
    private HandlerContext head;
    private HandlerContext tail;

    private Logger logger;

    private int capacity = 64;

    private boolean direct = false;

    public BasePipeline(boolean isDirect,int inBufferCap){
        this.direct = isDirect;
        this.capacity = inBufferCap;
        DefaultHandler defaultHandler = new DefaultHandler();
        this.addLast("DefHd",defaultHandler);
    }

    public BasePipeline(){

        this(false,1024*4);
    }


    public void flush(){
        if (!this.writeQueue.isEmpty()){
            this.flushTail = this.writeQueue.peekLast();
        }
    }

    public void write(ByteBuffer buffer,WritePromise promise){
        this.writeQueue.addLast(new PromiseEntry<>(buffer,promise));
    }

    public void doWrite(SelectionKey key) throws IOException {
        if (flushTail==null)
            return;
        if (!this.writeQueue.isEmpty()){
            PromiseEntry<ByteBuffer> curEntry = this.writeQueue.peekFirst();
            PromiseEntry<ByteBuffer> preEntry;
            do{
                ByteBuffer payload = curEntry.getPayload();
                int totalLen = payload.remaining();
                int wroteLen = this.socketChannel.write(payload);
                if (totalLen>wroteLen){
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    return;
                }else{
                    this.writeQueue.pollFirst();
                    curEntry.getPromise().setSuccess0();
                }
                preEntry = curEntry;
                curEntry = this.writeQueue.peekFirst();
            }while(preEntry!=this.flushTail&&!this.writeQueue.isEmpty());
            this.flushTail = null;
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
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

    private void addContext(String id,HandlerContext ctx){
        this.ctxMap.put(id,ctx);
        ctx.getHandler().onAdded(ctx);
    }

    private HandlerContext removeHandlerContext(String id){
        HandlerContext rmCtx = ctxMap.remove(id);
        rmCtx.getHandler().onRemoved(rmCtx);
        return rmCtx;
    }

    public BasePipeline addLast(String id, SimpleHandler<?,?> handler){
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
        this.addContext(id,newCtx);
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

    public BasePipeline addHead(String id,SimpleHandler<?,?> handler){
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
        this.addContext(id,newCtx);
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
            reCtx = removeHandlerContext(id);
        }
        return reCtx;
    }

    public HandlerContext getContext(String id){
        return ctxMap.get(id);
    }

        public BasePipeline addBefore(String tarId, String id, SimpleHandler<?,?> newHandler) {
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


    public BasePipeline addAfter(String tarId, String id,SimpleHandler<?,?> newHandler) {
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
        this.addContext(id,newCtx);
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

    /**
     * 触发管道中所有处理器的销毁事件，按照链表顺序执行
     * 每个处理器先执行删除，然后执行销毁
     */
    public void fireInterrupt(){
        HandlerContext p = head;
        while(p!=null){
            removeContext(p.getId());
            p.getHandler().onDestroy(p);
            p = p.getNext();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        HandlerContext curCtx = head;
        while (curCtx!=null){
            builder.append(curCtx.getId());
            builder.append("->");
            curCtx = curCtx.getNext();
        }
        return builder.toString();
    }

    public void registerExecutor(HandlerContext ctx, Executor executor){
        executorQueue.addLast(new AbstractMap.SimpleEntry<>(ctx,executor));
    }

    public void doExecutors(){
        if (executorQueue.isEmpty())
            return;
        Map.Entry<HandlerContext, Executor> entry;
        while (!executorQueue.isEmpty()){
            entry = executorQueue.poll();
            HandlerContext ctx = entry.getKey();
            Executor executor = entry.getValue();
            executor.run(ctx);
        }
    }
}
