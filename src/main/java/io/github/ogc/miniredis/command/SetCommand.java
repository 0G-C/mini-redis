package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisString;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * SET key value
 * <p>存入一个 String 类型的 key,返回 {@code +OK}。
 */
@CommandName("SET")
public class SetCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'set'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        String value = ((RespBulkString) args.get(2)).getValue();
        db.put(key, new RedisString(value));
        return new RespSimpleString("OK");
    }
}
