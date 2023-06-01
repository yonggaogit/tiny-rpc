package org.rpc.remoting.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.rpc.compress.Compress;
import org.rpc.enums.CompressTypeEnum;
import org.rpc.enums.SerializationTypeEnum;
import org.rpc.extension.ExtensionLoader;
import org.rpc.remoting.constants.RpcConstants;
import org.rpc.remoting.dto.RpcMessage;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.serialize.Serializer;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage, ByteBuf byteBuf) throws Exception {
        byteBuf.writeBytes(RpcConstants.MAGIC_NUMBER);
        byteBuf.writeByte(RpcConstants.VERSION);
        byteBuf.writerIndex(byteBuf.writerIndex() + 4);
        byte messageType = rpcMessage.getMessageType();
        byteBuf.writeByte(messageType);
        byteBuf.writeByte(rpcMessage.getCodec());
        byteBuf.writeByte(CompressTypeEnum.GZIP.getCode());
        byteBuf.writeInt(ATOMIC_INTEGER.getAndIncrement());

        byte[] bodyBytes = null;
        int fullLength = RpcConstants.HEAD_LENGTH;
        if ( messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE ) {
            String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());

            log.info("codec name: [{}]", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            bodyBytes = serializer.serialize( rpcMessage.getData() );

            String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
            Compress compress = ExtensionLoader.getExtensionLoader( Compress.class ).getExtension(compressName);
            bodyBytes = compress.compress( bodyBytes );
            fullLength += bodyBytes.length;
        }

        if ( bodyBytes != null ) {
            byteBuf.writeBytes( bodyBytes );
        }

        int writeIndex = byteBuf.writerIndex();
        byteBuf.writerIndex( writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1 );
        byteBuf.writeInt( fullLength );
        byteBuf.writerIndex( writeIndex );
    }
}
