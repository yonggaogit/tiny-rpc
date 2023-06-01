package org.rpc.remoting.dto;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Builder
@ToString
@AllArgsConstructor
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1283891293901022379L;

    private String requestId;
    private String interfaceName;
    private String methodName;
    private Object[] parameters;
    private Class<?>[] paramTypes;
    private String version;
    private String group;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
