package org.rpc.loadbalance;

import org.rpc.remoting.dto.RpcRequest;

import java.util.List;

public interface LoadBalance {
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
