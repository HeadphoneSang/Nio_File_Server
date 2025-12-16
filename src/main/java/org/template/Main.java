package org.template;

import org.template.server.NServer;
import org.template.server.components.BasePipeline;
import org.template.server.components.handlers.ByteEncodeHandler;
import org.template.server.components.handlers.DecisionHandler;
import org.template.server.components.handlers.SimpleBufferDecodeHandler;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        NServer server = new NServer(25565);

        try {
            server.accept(()->{
                BasePipeline pipeline = new BasePipeline(true,4*1024);
                pipeline.
                        addLast("simpleHandler",new SimpleBufferDecodeHandler()).
                        addLast("encodeHandler",new ByteEncodeHandler()).
                        addLast("decision",new DecisionHandler());
                System.out.println(pipeline);
                return pipeline;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}