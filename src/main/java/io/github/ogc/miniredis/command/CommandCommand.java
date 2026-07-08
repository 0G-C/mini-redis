package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespObject;

import java.util.List;

/**
 * COMMAND 命令 -- redis-cli 6+ 连上来自动发,拉命令元数据做补全。
 * W1/W2 返回空数组让 cli 安静。后续可实现 COUNT/DOCS/LIST/INFO。
 */
@CommandName("COMMAND")
public class CommandCommand implements Command {

    private static final RespArray EMPTY_ARRAY = new RespArray(List.of());

    @Override
    public RespObject execute(RedisDb db, RespArray args) {
        return EMPTY_ARRAY;
    }
}
