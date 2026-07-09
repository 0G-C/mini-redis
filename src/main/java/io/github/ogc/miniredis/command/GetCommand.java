package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.core.object.RedisString;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;

@CommandName("GET")
public class GetCommand implements Command {

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        if (args.size() != 2) {
            return new RespSimpleString("ERR wrong number of arguments for 'get'");
        }

        RespBulkString keyBulk = (RespBulkString) args.get(1);
        RedisString result = (RedisString) db.get(keyBulk.getValue());

        if (result == null) {
            return RespBulkString.NULL;
        }
        return new RespBulkString(result.getValue());
    }
}
