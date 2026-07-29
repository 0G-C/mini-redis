package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisList;
import io.github.ogc.miniredis.core.object.RedisObject;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespInteger;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

import java.util.LinkedList;

/**
 * LRANGE key start stop
 * <p>返回 List 中指定范围 [start, stop] 的元素。支持负索引(-1 表示最后一个)。
 */
@CommandName("LRANGE")
public class LrangeCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 4) {
            return new RespSimpleString("ERR wrong number of arguments for 'lrange'");
        }

        String key = ((RespBulkString) args.get(1)).getValue();
        int start, stop;
        try {
            start = Integer.parseInt(((RespBulkString) args.get(2)).getValue());
            stop = Integer.parseInt(((RespBulkString) args.get(3)).getValue());
        } catch (NumberFormatException e) {
            return new RespSimpleString("ERR value is not an integer or out of range");
        }

        RedisObject existing = db.get(key);
        if (existing == null) {
            return new RespArray(java.util.List.of());
        }
        if (!(existing instanceof RedisList list)) {
            return new RespSimpleString("WRONGTYPE Operation against a key holding the wrong kind of value");
        }

        LinkedList<String> elements = list.getElements();
        int size = elements.size();
        if (size == 0) {
            return new RespArray(java.util.List.of());
        }

        // 负索引转正
        if (start < 0) start = Math.max(0, size + start);
        if (stop < 0) stop = size + stop;
        // 边界约束
        start = Math.max(0, start);
        stop = Math.min(size - 1, stop);

        if (start > stop) {
            return new RespArray(java.util.List.of());
        }

        java.util.List<RespObject> result = new java.util.ArrayList<>(stop - start + 1);
        int i = 0;
        for (String e : elements) {
            if (i >= start && i <= stop) {
                result.add(new RespBulkString(e));
            }
            if (i > stop) break;
            i++;
        }
        return new RespArray(result);
    }
}
