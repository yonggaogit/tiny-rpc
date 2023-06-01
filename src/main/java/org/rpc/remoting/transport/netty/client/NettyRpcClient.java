package org.rpc.remoting.transport.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rpc.enums.CompressTypeEnum;
import org.rpc.enums.SerializationTypeEnum;
import org.rpc.enums.ServiceDiscoveryEnum;
import org.rpc.extension.ExtensionLoader;
import org.rpc.factory.SingletonFactory;
import org.rpc.registry.ServiceDiscovery;
import org.rpc.remoting.dto.RpcMessage;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.remoting.dto.RpcResponse;
import org.rpc.remoting.transport.RpcRequestTransport;
import org.rpc.remoting.transport.netty.codec.RpcMessageDecoder;
import org.rpc.remoting.transport.netty.codec.RpcMessageEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyRpcClient implements RpcRequestTransport {
    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;

    public NettyRpcClient() {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();

        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        ChannelPipeline pipeline = nioSocketChannel.pipeline();
                        pipeline.addLast( new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        pipeline.addLast( new RpcMessageEncoder());
                        pipeline.addLast( new RpcMessageDecoder());
                        pipeline.addLast( new NettyRpcClientHandler() );
                    }
                });
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(ServiceDiscoveryEnum.ZK.getName());
        this.unprocessedRequests = SingletonFactory.getInstance( UnprocessedRequests.class );
        this.channelProvider = SingletonFactory.getInstance( ChannelProvider.class );
    }
    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();

        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        Channel channel = getChannel( inetSocketAddress );

        if ( channel.isActive() ) {
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage
                    .builder()
                    .data(rpcRequest)
                    .codec(SerializationTypeEnum.KYRO.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .build();

            channel.writeAndFlush(rpcMessage).addListener( ( ChannelFutureListener ) future -> {
                if ( future.isSuccess() ) {
                    log.info( "client send message: [{}]", rpcMessage );
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error( "Send failed:", future.cause() );
                }
            } );
        } else {
            throw new IllegalStateException();
        }

        return resultFuture;
    }

    @SneakyThrows
    public Channel doConnect( InetSocketAddress inetSocketAddress ) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect( inetSocketAddress ).addListener( (ChannelFutureListener) future -> {
            if ( future.isSuccess() ) {
                log.info( "The client has connected [{}] successful!", inetSocketAddress.toString() );
                completableFuture.complete( future.channel() );
            } else {
                throw new IllegalStateException();
            }
        } );

        return completableFuture.get();
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if ( channel == null ) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set( inetSocketAddress, channel );
        }
        return channel;
    }

    public void close() { eventLoopGroup.shutdownGracefully(); }
}
