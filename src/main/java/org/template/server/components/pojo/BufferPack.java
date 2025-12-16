package org.template.server.components.pojo;

import java.nio.ByteBuffer;

public class BufferPack extends Datapack<ByteBuffer>{
    public BufferPack(int opCode, ByteBuffer data) {
        super(opCode, data);
    }
}
