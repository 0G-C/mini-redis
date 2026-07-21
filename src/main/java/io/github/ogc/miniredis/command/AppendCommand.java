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
 * APPEND key value
 * <p>key 不存在则等同 SET;存在则在原值末尾追加,返回追加后总长度。
 */
@CommandName("APPEND")
public class AppendCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'append'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        String suffix = ((RespBulkString) args.get(2)).getValue();
        RedisObject existing = db.get(key);

        String newValue;
        if (existing == null) {
            newValue = suffix;
            db.put(key, new RedisString(newValue));
        } else {
            if (!(existing instanceof RedisString s)) {
                return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
            }
            newValue = s.getValue() + suffix;
            s.setValue(newValue);
        }

        return new RespInteger(newValue.length());
    }
}
