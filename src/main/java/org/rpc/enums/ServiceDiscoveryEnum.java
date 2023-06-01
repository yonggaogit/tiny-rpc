package org.rpc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public enum ServiceDiscoveryEnum {
    ZK("zk");
    private final String name;
}
