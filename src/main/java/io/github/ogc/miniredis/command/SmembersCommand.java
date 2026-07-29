package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.core.object.RedisSet;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

import java.util.ArrayList;
import java.util.List;

/**
 * SMEMBERS key
 * <p>返回 Set 中所有成员。key 不存在返回空 Array。
 */
@CommandName("SMEMBERS")
public class SmembersCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'smembers'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return new RespArray(List.of());
        }
        if (!(existing instanceof RedisSet set)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        List<RespObject> items = new ArrayList<>(set.getMembers().size());
        for (String member : set.getMembers()) {
            items.add(new RespBulkString(member));
        }
        return new RespArray(items);
    }
}
