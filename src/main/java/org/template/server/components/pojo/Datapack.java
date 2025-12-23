package org.template.server.components.pojo;

import org.template.server.components.abstracts.ToByte;
import org.template.server.utils.EncodeUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Datapack<T> {
    public static final int Upload = 1;

    public static final int Upload_ACK = 5;

    public static final int ReUpload_ACK = 6;

    public static final int Upload_Chunk = 4;

    public static final int Download = 2;

    public static final int String_Send = 3;

    private static final Map<Class<? extends Datapack<?>>, List<Field>> EncodeFields = new HashMap<>();

    private static final Map<Class<? extends Datapack<?>>,Integer> BufferLength = new HashMap<>();

    private static final Map<Class<? extends Datapack<?>>, List<Field>> EncodeStringFields = new HashMap<>();

    private ByteBuffer fieldBuffer;

    private final int op_code;

    private T data;

    public Datapack(int opCode,T data){
        this.data = data;
        this.op_code = opCode;
        Class<? extends Datapack<?>> clazz = (Class<? extends Datapack<?>>) getClass();
        if (!EncodeFields.containsKey(clazz)){
            Field[] fields = getAllAttributes();
            Annotation[] annotations;
            ArrayList<Field> filteredAllFields = new ArrayList<>();
            ArrayList<Field> filteredStringFields = new ArrayList<>();
            int len = 0;
            for (Field field : fields){
                annotations = field.getDeclaredAnnotations();
                for (Annotation annotation : annotations){
                    if (annotation.annotationType() == ToByte.class){
                        field.setAccessible(true);
                        filteredAllFields.add(field);
                        if (field.getType().isPrimitive()) {
                            if (field.getType().equals(byte.class)) {
                                len++;
                            } else if (field.getType().equals(int.class)) {
                                len+=4;
                            } else if (field.getType().equals(long.class)) {
                                len+=8;
                            } else if (field.getType().equals(boolean.class)) {
                                len+=1;
                            } else {
                                throw new IllegalStateException("Unexpected value: " + field.getType());
                            }
                        }else if (field.getType() == String.class)
                            filteredStringFields.add(field);
                    }
                }
            }
            BufferLength.put(clazz,len);
            EncodeFields.put(clazz,filteredAllFields);
            EncodeStringFields.put(clazz,filteredStringFields);
        }
    }
    public int getOperator(){
        return op_code;
    }

    public T getData(){
        return data;
    }

    private Field[] getAllAttributes(){
        return getClass().getDeclaredFields();
    }

    public ByteBuffer toByteBuffer(boolean isDirect) throws IllegalAccessException {
        Class<? extends Datapack<?>> clazz = (Class<? extends Datapack<?>>) getClass();
        List<Field> filteredStringFields = Datapack.EncodeStringFields.get(clazz);
        List<Field> filteredAllFields = Datapack.EncodeFields.get(clazz);
        List<byte[]> strCache = new ArrayList<>();
        int bufferSize = Datapack.BufferLength.get(clazz);
        for (Field field : filteredStringFields){
            String str = (String) field.get(this);
            byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
            strCache.add(strBytes);
            bufferSize+=strBytes.length;
            bufferSize+=4;
        }
        if ((fieldBuffer==null) || (fieldBuffer.isDirect() != isDirect))
            fieldBuffer = isDirect ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
        fieldBuffer.clear();
        int strBytesIndex = 0;
        for (Field field : filteredAllFields){
            if (field.getType().isPrimitive()) {
                if (field.getType().equals(byte.class)) {
                    fieldBuffer.put(field.getByte(this));
                } else if (field.getType().equals(int.class)) {
                    fieldBuffer.put(EncodeUtils.encodeToBigBytes(field.getInt(this),4));
                } else if (field.getType().equals(long.class)) {
                    fieldBuffer.put(EncodeUtils.encodeToBigBytes(field.getLong(this)));
                } else if (field.getType().equals(boolean.class)) {
                    byte p = (byte) (field.getBoolean(this)?1:0);
                    fieldBuffer.put(p);
                } else {
                    throw new IllegalStateException("Unexpected value: " + field.getType());
                }
            }else if (field.getType() == String.class){
                byte[] strBytes = strCache.get(strBytesIndex++);
                fieldBuffer.put(EncodeUtils.encodeToBigBytes(strBytes.length,4));
                fieldBuffer.put(strBytes);
            }
        }
        fieldBuffer.flip();
        return fieldBuffer;
    }

    protected void setData(T data){
        this.data = data;
    }

}
