package org.template.server.components.internals;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleThreadFactory implements ThreadFactory {

    private final String prefix;

    private final boolean daemon;

    private final AtomicInteger index = new AtomicInteger(0);



    SimpleThreadFactory(String prefix,boolean daemon){
        this.prefix = prefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName(prefix + "-" + index.getAndIncrement());
        t.setDaemon(daemon);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

}
