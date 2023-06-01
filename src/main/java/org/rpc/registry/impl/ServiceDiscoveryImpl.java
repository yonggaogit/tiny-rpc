package org.rpc.registry.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.rpc.loadbalance.LoadBalance;
import org.rpc.loadbalance.loadbalancer.RandomLoadBalance;
import org.rpc.registry.ServiceDiscovery;
import org.rpc.registry.impl.util.CuratorUtils;
import org.rpc.remoting.dto.RpcRequest;
import org.rpc.utils.CollectionUtil;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ServiceDiscoveryImpl() {
        this.loadBalance = new RandomLoadBalance();
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtil.isEmpty( serviceUrlList )) {
            log.error("load rpc service [{}] fail", rpcServiceName);
        }
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);

        return new InetSocketAddress(host, port);
    }
}
