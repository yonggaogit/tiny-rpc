package org.rpc.registry.impl;

import org.apache.curator.framework.CuratorFramework;
import org.rpc.registry.ServiceRegistry;
import org.rpc.registry.impl.util.CuratorUtils;

import java.net.InetSocketAddress;

public class ServiceRegistryImpl implements ServiceRegistry {
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZOOKEEPER_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
