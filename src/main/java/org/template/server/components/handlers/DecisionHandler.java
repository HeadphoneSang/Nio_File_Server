package org.template.server.components.handlers;

import org.template.server.components.handlers.subHandlers.EstablishUploadHandler;
import org.template.server.components.handlers.subHandlers.ReUploadHandler;
import org.template.server.components.handlers.subHandlers.SimpleSubHandler;
import org.template.server.components.handlers.subHandlers.UploadChunkHandler;
import org.template.server.components.internals.HandlerContext;
import org.template.server.components.internals.WritePromise;
import org.template.server.components.pojo.*;
import org.template.server.utils.DecodeUtils;
import org.template.server.utils.EncodeUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DecisionHandler extends SimpleHandler<BufferPack,ObjPack>{

    private final Map<Integer, SimpleSubHandler<BufferPack>> subHandlers = new HashMap<>();

    private void initSubHandlers(){
        subHandlers.put(Datapack.Upload_Chunk_SYN,new UploadChunkHandler());
        subHandlers.put(Datapack.Upload,new EstablishUploadHandler());
        subHandlers.put(Datapack.RE_UPLOAD_EST,new ReUploadHandler());
    }

    @Override
    public void onAdded(HandlerContext ctx) {
        ctx.enableRead();
        ctx.disableWrite();
        this.initSubHandlers();
    }

    @Override
    public void write(HandlerContext ctx, WritePromise promise, ObjPack msg) {

    }

    @Override
    public void onRemoved(HandlerContext ctx) {
        ctx.getLogger().info("removed");
    }

    @Override
    public void onDestroy(HandlerContext ctx) {
        ctx.getLogger().info("destroyed");
    }

    @Override
    public void channelRead0(HandlerContext ctx, BufferPack msg) {
        if (subHandlers.containsKey(msg.getOperator()))
            subHandlers.get(msg.getOperator()).handler(ctx,msg);
    }
}
