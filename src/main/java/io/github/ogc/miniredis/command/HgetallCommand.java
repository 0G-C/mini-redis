package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisHash;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

import java.util.ArrayList;
import java.util.List;

/**
 * HGETALL key
 * <p>返回 Hash 中所有 field-value 对,按 field1, value1, field2, value2, ... 排列。
 * <p>key 不存在返回空 Array。
 */
@CommandName("HGETALL")
public class HgetallCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'hgetall'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();

        RedisObject existing = db.get(key);
        if (existing == null) {
            return new RespArray(List.of());
        }
        if (!(existing instanceof RedisHash hash)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        // 按 field, value, field, value, ... 展平
        // 用 List<RespObject> 以匹配 RespArray 构造器签名
        List<RespObject> items = new ArrayList<>(hash.getFields().size() * 2);
        hash.getFields().forEach((field, value) -> {
            items.add(new RespBulkString(field));
            items.add(new RespBulkString(value));
        });

        return new RespArray(items);
    }
}
