package org.template.server.components.abstracts;

import org.template.server.components.internals.HandlerContext;

public interface Executor {
    void run(HandlerContext ctx);
}
