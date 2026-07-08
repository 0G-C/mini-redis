package io.github.ogc.miniredis.core.object;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis Hash 类型 -- field-value 映射。
 * <p>对应命令:HSET/HGET/HDEL/HGETALL/HLEN(W2)。
 */
@Getter
public class RedisHash extends RedisObject {
    private final Map<String, String> fields = new HashMap<>();
}
