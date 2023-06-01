package org.rpc.loadbalance.loadbalancer;

import org.rpc.loadbalance.AbstractLoadBalance;
import org.rpc.remoting.dto.RpcRequest;

import java.util.List;

public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    @Override
    public String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest) {
        return null;
    }
}
