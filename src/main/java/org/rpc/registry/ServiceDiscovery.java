package org.rpc.registry;

import org.rpc.remoting.dto.RpcRequest;

import java.net.InetSocketAddress;

public interface ServiceDiscovery {
    /**
     * lookup service by rpcServiceName
     *
     * @param rpcRequest rpc service pojo
     * @return service address
     */
    InetSocketAddress lookupService( RpcRequest rpcRequest );
}
