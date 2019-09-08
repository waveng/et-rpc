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
public class EtRpcRequest {
    /**
     * 请求编号
     */
    private String requestId;

    /**
     * 接口名称
     */
    private String interfaceName;
    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 参数类型
     */
    private Class<?>[] parameterTypes;

    /**
     * 参数对象
     */
    private Object[] parameters;

}
