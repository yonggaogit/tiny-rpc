package org.rpc.serialize.kyro;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.remoting.dto.RpcResponse;
import org.rpc.serialize.Serializer;

import com.esotericsoftware.kryo.Kryo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
public class KryoSerializer implements Serializer {
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(()->{
       Kryo kryo = new Kryo();
       kryo.register(RpcRequest.class);
       kryo.register(RpcResponse.class);
       return kryo;
    });
    @Override
    public byte[] serialize(Object object) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(output, object);
            kryoThreadLocal.remove();
            return output.toBytes();
        } catch ( Exception e ) {
            throw new RuntimeException("Serialization failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( bytes );
             Input input = new Input( byteArrayInputStream )) {
            Kryo kryo = kryoThreadLocal.get();
            Object o = kryo.readObject(input, clazz);
            kryoThreadLocal.remove();
            return clazz.cast(o);
        } catch ( Exception e ) {
            throw new RuntimeException("Deserialization failed");
        }
    }
}
