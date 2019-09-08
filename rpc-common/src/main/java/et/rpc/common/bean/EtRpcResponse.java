package et.rpc.common.bean;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author wangl
 */
@Builder
@Getter
@Setter
public class EtRpcResponse {
    /**
     * 请求编号
     */
    private String requestId;
    /**
     * 异常信息
     */
    private Exception exception;
    /**
     * 响应结果
     */
    private Object result;

    /**
     * 是否带有异常信息
     */
    public boolean hasException(){
        return exception != null;
    }
}
