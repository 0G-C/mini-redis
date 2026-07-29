package io.github.ogc.miniredis.core.object;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis Set 类型 —— 无序不重复集合。
 * <p>内部用 {@link HashSet},和 Redis 官方用 dict 做 set 的语义一致(只有 key,没有 value)。
 */
@Getter
public class RedisSet extends RedisObject {
    private final Set<String> members = new HashSet<>();
}
