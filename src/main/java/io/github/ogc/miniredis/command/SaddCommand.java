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
 * SADD key member [member ...]
 * <p>向 Set 中添加一个或多个成员。返回新增成员数(已存在的不计)。
 */
@CommandName("SADD")
public class SaddCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() < 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'sadd'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        RedisSet set;
        if (existing == null) {
            set = new RedisSet();
            db.put(key, set);
        } else if (existing instanceof RedisSet s) {
            set = s;
        } else {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        long added = 0;
        for (int i = 2; i < args.size(); i++) {
            if (set.getMembers().add(((RespBulkString) args.get(i)).getValue())) {
                added++;
            }
        }
        return new RespInteger(added);
    }
}
