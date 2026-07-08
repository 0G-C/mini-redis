package io.github.ogc.miniredis.handler;

import io.github.ogc.miniredis.command.CommandDispatcher;
import io.github.ogc.miniredis.resp.RespObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 命令处理器 -- 网络层,只负责把 RESP 对象交给 {@link CommandDispatcher} 分发,再把响应写回。
 * 命令路由逻辑全部下沉到 dispatcher + Command 类。
 *
 * <p>W1 版本的 switch 分派已移除,改成查注册表多态分派。
 * 加新命令不再改这里,写个 Command 类贴 {@code @CommandName} 即可。
 */
@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<RespObject> {

    private final CommandDispatcher dispatcher = CommandDispatcher.getInstance();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RespObject msg) {
        RespObject response = dispatcher.dispatch(msg);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("channel exception, closing: {}", ctx.channel(), cause);
        ctx.close();
    }
}
