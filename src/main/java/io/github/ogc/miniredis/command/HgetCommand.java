package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisHash;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * HGET key field
 * <p>返回 Hash 中指定 field 的值。key 或 field 不存在返回 nil。
 */
@CommandName("HGET")
public class HgetCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'hget'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        String field = ((RespBulkString) args.get(2)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return RespBulkString.NULL;
        }
        if (!(existing instanceof RedisHash hash)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        String value = hash.getFields().get(field);
        if (value == null) {
            return RespBulkString.NULL;
        }
        return new RespBulkString(value);
    }
}
