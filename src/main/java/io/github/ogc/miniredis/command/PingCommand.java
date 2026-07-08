package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * PING 命令。
 * <ul>
 *   <li>{@code PING} -> {@code +PONG}</li>
 *   <li>{@code PING <msg>} -> 原样返回 msg</li>
 * </ul>
 * PONG 用 static final 常量,避免高频命令每次 new。
 */
@CommandName("PING")
public class PingCommand implements Command {

    private static final RespSimpleString PONG = new RespSimpleString("PONG");

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        return switch (args.size()) {
            case 1 -> PONG;
            case 2 -> args.get(1);  // 原样回参数
            default -> new RespSimpleString("ERR wrong number of arguments for 'ping'");
        };
    }
}
