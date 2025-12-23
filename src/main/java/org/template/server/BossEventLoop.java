package org.template.server;

import org.template.server.components.BasePipeline;
import org.template.server.components.InitPipeline;

import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BossEventLoop extends EventLoop{
    BossEventLoop(InitPipeline initPipeHandler) {
        super(initPipeHandler);
    }

    @Override
    protected void loopEpoch(Iterator<SelectionKey> readyEvents, List<Map.Entry<SelectionKey, BasePipeline>> curPipes) {

    }
}
