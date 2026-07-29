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
 * LPUSH key element [element ...]
 * <p>将一个或多个元素插入 List 头部(左侧)。返回插入后列表长度。
 */
@CommandName("LPUSH")
public class LpushCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() < 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'lpush'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        RedisList list;
        if (existing == null) {
            list = new RedisList();
            db.put(key, list);
        } else if (existing instanceof RedisList l) {
            list = l;
        } else {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        // 从左端依次插入,最后入的元素在最左(Redis 行为)
        for (int i = 2; i < args.size(); i++) {
            list.getElements().addFirst(((RespBulkString) args.get(i)).getValue());
        }

        return new RespInteger(list.getElements().size());
    }
}
