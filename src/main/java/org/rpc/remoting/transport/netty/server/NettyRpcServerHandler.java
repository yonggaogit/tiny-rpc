package org.rpc.remoting.transport.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.rpc.enums.CompressTypeEnum;
import org.rpc.enums.SerializationTypeEnum;
import org.rpc.remoting.constants.RpcConstants;
import org.rpc.remoting.dto.RpcMessage;
import org.rpc.remoting.dto.RpcRequest;

public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if ( msg instanceof RpcMessage ) {
            byte messageType = ( ( RpcMessage ) msg ).getMessageType();
            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setCodec(SerializationTypeEnum.KYRO.getCode());
            rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());

            if ( messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                rpcMessage.setData(RpcConstants.PONG);
            } else {
                RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();

            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }
}
