package org.template.server.components.handlers;

import org.template.server.components.HandlerContext;
import org.template.server.components.WritePromise;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class SimpleHandler<T,K> {


    public boolean channelRead(HandlerContext ctx, Object msg){
        if(isReadGenericType(msg)){
            @SuppressWarnings("unchecked")
            T readObj = (T)msg;
            this.channelRead0(ctx,readObj);
            return true;
        }
        return false;
    }

    public abstract void write(HandlerContext ctx, WritePromise promise,K msg);

    public boolean channelWrite(HandlerContext ctx,WritePromise promise,Object msg){
        if(isWriteGenericType(msg)){
            @SuppressWarnings("unchecked")
            K writeObj = (K)msg;
            this.write(ctx,promise,writeObj);
            return true;
        }
        return false;
    }

    public boolean isReadGenericType(Object msg){
        ParameterizedType pType =  (ParameterizedType) this.getClass().getGenericSuperclass();
        Type[] types = pType.getActualTypeArguments();
        Type genType = types[0];
        return judge(msg, genType);
    }

    public boolean isWriteGenericType(Object msg){
        ParameterizedType pType =  (ParameterizedType) this.getClass().getGenericSuperclass();
        Type[] types = pType.getActualTypeArguments();
        Type genType = types[1];
        return judge(msg, genType);
    }

    private boolean judge(Object msg, Type genType) {
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


    public abstract void onRemoved(HandlerContext ctx);

    public abstract void onAdded(HandlerContext ctx);

    public abstract void onDestroy(HandlerContext ctx);

    public abstract void channelRead0(HandlerContext ctx,T msg);


}
