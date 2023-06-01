package org.rpc.remoting.dto;

import lombok.*;
import org.rpc.enums.RpcResponseCodeEnum;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
@Builder
@Setter
public class RpcResponse<T> implements Serializable {
    private static final long serialVersionUID = 21893821371294012L;

    private String requestId;
    private Integer code;
    private String message;

    private T data;

    public static <T> RpcResponse<T> success( String requestId, T data ) {
        RpcResponse<T> rpcResponse = new RpcResponse<T>();
        rpcResponse.setCode(RpcResponseCodeEnum.SUCCESS.getCode());
        rpcResponse.setMessage(RpcResponseCodeEnum.SUCCESS.getMessage());
        rpcResponse.setRequestId(requestId);
        if ( data != null ) {
            rpcResponse.setData( data );
        }
        return rpcResponse;
    }

    public static <T> RpcResponse<T> fail() {
        RpcResponse<T> rpcResponse = new RpcResponse<T>();
        rpcResponse.setCode(RpcResponseCodeEnum.FAIL.getCode());
        rpcResponse.setMessage(RpcResponseCodeEnum.FAIL.getMessage());

        return rpcResponse;
    }
}
