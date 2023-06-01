package org.rpc.provider;

import org.rpc.config.RpcServiceConfig;

public interface ServiceProvider {
    void addService( RpcServiceConfig rpcServiceConfig );

    Object getService( String rpcServiceName );

    void publishService( RpcServiceConfig rpcServiceConfig );
}
