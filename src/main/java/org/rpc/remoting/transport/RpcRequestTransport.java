package org.rpc.remoting.transport;

import org.rpc.remoting.dto.RpcRequest;

public interface RpcRequestTransport {
    Object sendRpcRequest(RpcRequest rpcRequest);
}
