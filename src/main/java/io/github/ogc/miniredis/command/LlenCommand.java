package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisList;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * LLEN key
 * <p>返回 List 的长度。key 不存在返回 0。
 */
@CommandName("LLEN")
public class LlenCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'llen'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return new RespInteger(0);
        }
        if (!(existing instanceof RedisList list)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        return new RespInteger(list.getElements().size());
    }
}
