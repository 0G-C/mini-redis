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
 * DECR key
 * <p>将 key 存储的数字值减 1。key 不存在则设为 -1。
 */
@CommandName("DECR")
public class DecrCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'decr'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        RedisObject existing = db.get(key);

        long newValue;
        if (existing == null) {
            newValue = -1L;
            db.put(key, new RedisString(String.valueOf(newValue)));
        } else {
            if (!(existing instanceof RedisString s)) {
                return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
            }
            try {
                newValue = Long.parseLong(s.getValue()) - 1L;
            } catch (NumberFormatException e) {
                return new RespSimpleString("ERR value is not an integer or out of range");
            }
            s.setValue(String.valueOf(newValue));
        }

        return new RespInteger(newValue);
    }
}
