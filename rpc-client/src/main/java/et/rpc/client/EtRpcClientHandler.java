package et.rpc.client;

import et.rpc.common.bean.EtRpcResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
class EtRpcClientHandler extends SimpleChannelInboundHandler<EtRpcResponse> {
    @NonNull
    private Map<String, Response> responseMap;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, EtRpcResponse response) throws Exception {
        responseMap.get(response.getRequestId()).set(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        log.error("client caught exception", cause);
        ctx.close();
    }
}
