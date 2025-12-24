package org.template.server.components.internals;

import java.util.concurrent.atomic.AtomicInteger;

public class BossEventLoopGroup extends AbstractEventLoopGroup{

    private final AtomicInteger loopIndex = new AtomicInteger(0);
    public BossEventLoopGroup(int nThreads) {
        super(nThreads);
        eventLoops = new EventLoop[nThreads];
        factory = new SimpleThreadFactory("boss-thread",false);
    }

    @Override
    public EventLoop next() {
        synchronized (this){
            int curIndex = loopIndex.get();
            EventLoop loop = eventLoops[curIndex];
            if (loop==null){
                loop = new BossEventLoop();
                loop.regThreadFactory(factory);
                loop.setInitPipeHandler(initPipeline);
                eventLoops[curIndex] = loop;
            }
            curIndex = (curIndex+1)%nThreads;
            loopIndex.set(curIndex);
            return loop;
        }
    }
}
