package et.rpc.server;

import et.rpc.common.bean.EtRpcRequest;
import et.rpc.common.bean.EtRpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author wangl
 */
@Slf4j
@ChannelHandler.Sharable
public class EtRpcServerHandler extends SimpleChannelInboundHandler<EtRpcRequest> {

    private Map<String, Object> serviceMap;

    public EtRpcServerHandler(Map<String, Object> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, EtRpcRequest request) {
        EtRpcResponse.EtRpcResponseBuilder responseBuilder = EtRpcResponse.builder();
        responseBuilder.requestId(request.getRequestId());
        try {
            Object result = handle(request);
            responseBuilder.result(result);
        } catch (Exception e) {
            responseBuilder.exception(e);
            log.error("handle result failure", e);
        }
        ctx.writeAndFlush(responseBuilder.build()).addListener(ChannelFutureListener.CLOSE);

    }

    private Object handle(EtRpcRequest request) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String serviceName = request.getInterfaceName();
        Object serviceBean = serviceMap.get(serviceName);
        if (serviceBean == null) {
            throw new RuntimeException(String.format("can not find service bean by key: %s", serviceBean));
        }
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parmeters = request.getParameters();
        Method method = serviceClass.getMethod(methodName, parameterTypes);

        if(!method.isAccessible()) {
            method.setAccessible(true);
        }
        return method.invoke(serviceBean, parmeters);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server caughe exception", cause);
        ctx.close();
    }
}
