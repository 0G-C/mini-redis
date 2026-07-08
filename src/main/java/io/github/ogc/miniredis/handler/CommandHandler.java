package io.github.ogc.miniredis.handler;

import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 命令处理器 —— W1 版本,只处理 PING / ECHO / COMMAND(空实现)。
 *
 * <p>Pipeline 上游:{@code RespDecoder} 已经把字节流解成 {@link RespObject}。
 * <p>Pipeline 下游:{@code RespEncoder} 会把返回的 {@link RespObject} 编回字节。
 *
 * <p>redis-cli 发的命令一定是 {@link RespArray},第一个元素是命令名(BulkString)。
 * 例如 {@code PING} → {@code *1\r\n$4\r\nPING\r\n}
 *      {@code ECHO hello} → {@code *2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n}
 *
 * <p>redis-cli 6+ 连接建立时会自动发 {@code COMMAND} 拉命令元数据做补全,
 * 这里返回空数组让 cli 安静。W2 后可实现 COMMAND COUNT/DOCS/LIST/INFO。
 */
@Slf4j
public class CommandHandler extends SimpleChannelInboundHandler<RespObject> {

    private static final RespSimpleString PONG = new RespSimpleString("PONG");
    private static final RespSimpleString OK = new RespSimpleString("OK");
    private static final RespArray EMPTY_ARRAY = new RespArray(List.of());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RespObject msg) {
        // 客户端发的命令必是 Array,不是就忽略(协议不合规)
        if (!(msg instanceof RespArray array)) {
            log.warn("expected RespArray command, got {}", msg.getClass().getSimpleName());
            return;
        }
        if (array.size() == 0) {
            log.warn("empty command array");
            return;
        }
        // 第一个元素理论上是 BulkString,不是就丢
        if (!(array.get(0) instanceof RespBulkString cmdBulk) || cmdBulk.isNull()) {
            log.warn("command name is not a bulk string: {}", array.get(0));
            return;
        }

        String commandName = cmdBulk.getValue().toUpperCase();
        RespObject response = switch (commandName) {
            case "PING" -> handlePing(array);
            case "ECHO" -> handleEcho(array);
            case "COMMAND" -> EMPTY_ARRAY;  // redis-cli 补全用,W1 返回空
            default -> {
                log.warn("unknown command: {}", commandName);
                yield new RespSimpleString("ERR unknown command '" + commandName + "'");
            }
        };

        ctx.writeAndFlush(response);
    }

    private RespObject handlePing(RespArray array) {
        // PING          → +PONG
        // PING <arg>    → 原样返回 arg(BulkString)
        return switch (array.size()) {
            case 1 -> PONG;
            case 2 -> array.get(1);  // 直接把参数回给客户端
            default -> new RespSimpleString("ERR wrong number of arguments for 'ping'");
        };
    }

    private RespObject handleEcho(RespArray array) {
        // ECHO <arg>    → 原样返回 arg,size 必须是 2
        if (array.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'echo'");
        }
        return array.get(1);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // W1 简单处理:log + 关连接。
        // 后期可以判断协议异常返回 RESP Error,其他异常关闭。
        log.error("channel exception, closing: {}", ctx.channel(), cause);
        ctx.close();
    }
}
