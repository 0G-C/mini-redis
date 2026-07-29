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
 * SISMEMBER key member
 * <p>判断 member 是否在 Set 中。存在返回 1,不存在返回 0。
 */
@CommandName("SISMEMBER")
public class SismemberCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'sismember'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        String member = ((RespBulkString) args.get(2)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return new RespInteger(0);
        }
        if (!(existing instanceof RedisSet set)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        boolean exists = set.getMembers().contains(member);
        return new RespInteger(exists ? 1 : 0);
    }
}
