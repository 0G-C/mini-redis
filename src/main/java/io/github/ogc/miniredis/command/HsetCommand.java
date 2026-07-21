package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisHash;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

/**
 * HSET key field value
 * <p>设置 Hash 中指定 field 的值。key 不存在则创建新 Hash。
 * <p>返回本次操作新增的 field 数量(不统计覆盖的)。
 */
@CommandName("HSET")
public class HsetCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 4) {
            return new RespSimpleString("ERR wrong number of arguments for 'hset'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        String field = ((RespBulkString) args.get(2)).getValue();
        String value = ((RespBulkString) args.get(3)).getValue();

        RedisObject existing = db.get(key);
        RedisHash hash;

        if (existing == null) {
            hash = new RedisHash();
            db.put(key, hash);
        } else if (existing instanceof RedisHash h) {
            hash = h;
        } else {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        boolean isNew = hash.getFields().put(field, value) == null;
        return new RespInteger(isNew ? 1 : 0);
    }
}
