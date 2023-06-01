package org.rpc.loadbalance;

import org.rpc.remoting.dto.RpcRequest;
import org.rpc.utils.CollectionUtil;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            return null;
        }

        if ( serviceUrlList.size() == 1 ) {
            return serviceUrlList.get(0);
        } else {
            return doSelect( serviceUrlList, rpcRequest );
        }
    }

    public abstract String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest);
}
