package org.template.server.components.handlers.subHandlers;

import org.template.server.components.internals.HandlerContext;

import java.nio.ByteBuffer;

public interface SimpleSubHandler<T> {
    void handler(HandlerContext ctx, T msg);
}
