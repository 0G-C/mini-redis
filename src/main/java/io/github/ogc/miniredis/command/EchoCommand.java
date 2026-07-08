package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * ECHO 命令 -- 原样返回参数。
 * <p>{@code ECHO <msg>} -> msg,参数数必须是 2(命令名 + 1 个参数)。
 */
@CommandName("ECHO")
public class EchoCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'echo'");
        }
        return args.get(1);
    }
}
