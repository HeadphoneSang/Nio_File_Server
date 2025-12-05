package org.template;

import org.template.server.NServer;
import org.template.server.PreWriteHandler;
import org.template.server.components.BasePipeline;
import org.template.server.components.BufferHandler;
import org.template.server.components.HandlerContext;
import org.template.server.components.StringHandler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        NServer server = new NServer(25565);

        try {
            server.accept(()->{
                BasePipeline pipeline = new BasePipeline();
                pipeline.addLast("byte",new BufferHandler()).addLast("string",new StringHandler());
                pipeline.setCapacity(12);
                return pipeline;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}