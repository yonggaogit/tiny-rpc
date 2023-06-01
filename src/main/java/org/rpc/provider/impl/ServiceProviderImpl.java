package org.rpc.provider.impl;

import lombok.extern.slf4j.Slf4j;
import org.rpc.config.RpcServiceConfig;
import org.rpc.enums.ServiceRegistryEnum;
import org.rpc.extension.ExtensionLoader;
import org.rpc.provider.ServiceProvider;
import org.rpc.registry.ServiceRegistry;
import org.rpc.remoting.transport.netty.server.NettyRpcServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ServiceProviderImpl implements ServiceProvider {
    private final Map<String, Object> serviceMap;

    private final Set<String> registeredService;

    private final ServiceRegistry serviceRegistry;

    public ServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension(ServiceRegistryEnum.ZK.getName());
    }
    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if ( registeredService.contains( rpcServiceName ) ) {
            return;
        }

        registeredService.add( rpcServiceName );
        serviceMap.put( rpcServiceName, rpcServiceConfig.getService() );
        log.info("Add service: {} and interfaces: {}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if ( null == service ) {
            throw new RuntimeException("Service has not been registry");
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch ( UnknownHostException e ) {
            log.error( "occur exception when getHostAddress", e );
        }
    }
}
