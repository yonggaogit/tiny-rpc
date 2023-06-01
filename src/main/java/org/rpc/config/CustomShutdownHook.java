package org.rpc.config;

import lombok.extern.slf4j.Slf4j;
import org.rpc.registry.impl.util.CuratorUtils;
import org.rpc.remoting.transport.netty.server.NettyRpcServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Slf4j
public class CustomShutdownHook {
    private static final CustomShutdownHook CUSTOM_SHUTDOWN_HOOK = new CustomShutdownHook();

    public static CustomShutdownHook getCustomShutdownHook() { return CUSTOM_SHUTDOWN_HOOK; }

    public void clearAll() {
        log.info("addShutdownHook for clearAll");
        Runtime.getRuntime().addShutdownHook( new Thread(){
            @Override
            public void run() {
                try {
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyRpcServer.PORT);
                    CuratorUtils.clearRegistry(CuratorUtils.getZkClient(), inetSocketAddress);
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
