package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * EXPIRE key seconds
 * <p>设置 key 的过期时间(秒)。key 不存在返回 0,存在返回 1。
 * <p>底层存储毫秒时间戳,由 {@code RedisDb.get()} 在访问时做惰性删除。
 */
@CommandName("EXPIRE")
public class ExpireCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 3) {
            return new RespSimpleString("ERR wrong number of arguments for 'expire'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        long seconds;
        try {
            seconds = Long.parseLong(((RespBulkString) args.get(2)).getValue());
        } catch (NumberFormatException e) {
            return new RespSimpleString("ERR value is not an integer or out of range");
        }

        // get() 内自带惰性删除:已过期的 key 返回 null,不计为存在
        RedisObject obj = db.get(key);
        if (obj == null) {
            return new RespInteger(0);
        }

        // Redis 行为:非正秒数 → 立即删除
        if (seconds <= 0) {
            db.remove(key);
            return new RespInteger(1);
        }

        obj.setExpireAt(System.currentTimeMillis() + seconds * 1000L);
        return new RespInteger(1);
    }
}
