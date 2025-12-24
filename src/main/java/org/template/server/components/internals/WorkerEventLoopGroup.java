package org.template.server.components.internals;

import java.util.concurrent.atomic.AtomicInteger;

public class WorkerEventLoopGroup extends AbstractEventLoopGroup{

    private final AtomicInteger loopIndex = new AtomicInteger(0);

    public WorkerEventLoopGroup(int nThreads) {
//        super(nThreads *  Runtime.getRuntime().availableProcessors());
        super(nThreads);

        eventLoops = new EventLoop[this.nThreads];
        factory = new SimpleThreadFactory("worker-thread",false);
    }

    @Override
    public EventLoop next() {
        synchronized (this){
            int curIndex = loopIndex.get();
            EventLoop loop = eventLoops[curIndex];
            if (loop==null){
                loop = new WorkerEventLoop();
                loop.setInitPipeHandler(initPipeline);
                loop.regThreadFactory(factory);
                eventLoops[curIndex] = loop;
            }
            curIndex = (curIndex+1) % this.nThreads;
            loopIndex.set(curIndex);
            return loop;
        }
    }
}
