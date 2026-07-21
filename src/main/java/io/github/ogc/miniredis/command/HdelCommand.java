package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisHash;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * HDEL key field [field ...]
 * <p>删除 Hash 中指定的一或多个 field。返回实际被删除的数量。
 */
@CommandName("HDEL")
public class HdelCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() < 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'hdel'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return new RespInteger(0);
        }
        if (!(existing instanceof RedisHash hash)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        long count = 0;
        for (int i = 2; i < args.size(); i++) {
            String field = ((RespBulkString) args.get(i)).getValue();
            if (hash.getFields().remove(field) != null) {
                count++;
            }
        }
        return new RespInteger(count);
    }
}
