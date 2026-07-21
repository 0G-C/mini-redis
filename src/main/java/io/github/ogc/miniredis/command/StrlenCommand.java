package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.core.object.RedisString;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * STRLEN key
 * <p>返回 key 存储字符串的长度(字符数)。key 不存在返回 0。
 */
@CommandName("STRLEN")
public class StrlenCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'strlen'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        RedisObject existing = db.get(key);

        if (existing == null) {
            return new RespInteger(0);
        }
        if (!(existing instanceof RedisString s)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }
        return new RespInteger(s.getValue().length());
    }
}
