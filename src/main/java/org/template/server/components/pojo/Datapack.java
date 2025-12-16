package org.template.server.components.pojo;

public class Datapack<T> {

    public Datapack(int opCode,T data){
        this.data = data;
        this.op_code = opCode;
    }

    public static final int Upload = 1;

    public static final int Upload_ACK = 5;

    public static final int Upload_Chunk = 4;

    public static final int Download = 2;

    public static final int String_Send = 3;

    private int op_code;

    private T data;

    public int getOperator(){
        return op_code;
    }

    public T getData(){
        return data;
    }

}
