package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * DEL key [key ...]
 * <p>删除一个或多个 key,返回实际被删除的数量(不存在的 key 不计入)。
 */
@CommandName("DEL")
public class DelCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() < 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'del'");
        }

        long count = 0;
        for (int i = 1; i < args.size(); i++) {
            String key = ((RespBulkString) args.get(i)).getValue();
            if (db.remove(key) != null) {
                count++;
            }
        }
        return new RespInteger(count);
    }
}
