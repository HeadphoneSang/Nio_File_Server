package org.template.server.components;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class SimpleHandler<T> {


    boolean channelRead(HandlerContext ctx,Object msg){
        if(isGenericType(msg)){
            @SuppressWarnings("unchecked")
            T readObj = (T)msg;
            this.channelRead0(ctx,readObj);
            return true;
        }
        return false;
    }

    boolean channelWrite(HandlerContext ctx,Object msg){
        if(isGenericType(msg)){
            @SuppressWarnings("unchecked")
            T readObj = (T)msg;
            this.channelWrite0(ctx,readObj);
            return true;
        }
        return false;
    }

    boolean isGenericType(Object msg){
        ParameterizedType pType =  (ParameterizedType) this.getClass().getGenericSuperclass();
        Type[] types = pType.getActualTypeArguments();
        Type genType = types[0];
        if (genType instanceof Class){
            Class<?> clazz = (Class<?>) genType;
            return clazz.isInstance(msg);
        }else if(genType instanceof WildcardType){
            WildcardType wType = (WildcardType) genType;
            Type[] upperTypes = wType.getUpperBounds();
            if (upperTypes.length != 1)
                return false;
            else{
                Class<?> clazz = (Class<?>) upperTypes[0];
                return clazz.isInstance(msg);
            }
        }
        return false;
    }


    abstract void onRemoved(HandlerContext ctx);

    abstract void onAdded(HandlerContext ctx);

    abstract void channelRead0(HandlerContext ctx,T msg);

    abstract void channelWrite0(HandlerContext ctx,T msg);


}
