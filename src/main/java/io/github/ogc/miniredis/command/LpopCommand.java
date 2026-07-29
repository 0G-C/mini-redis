package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisList;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * LPOP key
 * <p>移除并返回 List 头部(左侧)的第一个元素。key 不存在或 List 为空返回 nil。
 */
@CommandName("LPOP")
public class LpopCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'lpop'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return RespBulkString.NULL;
        }
        if (!(existing instanceof RedisList list)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        if (list.getElements().isEmpty()) {
            return RespBulkString.NULL;
        }
        return new RespBulkString(list.getElements().removeFirst());
    }
}
