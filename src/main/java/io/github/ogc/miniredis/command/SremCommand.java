package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.core.object.RedisSet;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * SREM key member [member ...]
 * <p>从 Set 中移除一个或多个成员。返回实际移除数。
 */
@CommandName("SREM")
public class SremCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() < 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'srem'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return new RespInteger(0);
        }
        if (!(existing instanceof RedisSet set)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        long removed = 0;
        for (int i = 2; i < args.size(); i++) {
            if (set.getMembers().remove(((RespBulkString) args.get(i)).getValue())) {
                removed++;
            }
        }
        return new RespInteger(removed);
    }
}
