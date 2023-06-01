package org.rpc.serialize;

import org.rpc.extension.SPI;

@SPI
public interface Serializer {
    byte[] serialize( Object object );

    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
