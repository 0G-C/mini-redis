package io.github.ogc.miniredis.core.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Redis String 类型 -- 存一个字符串值。
 * <p>对应命令:GET/SET/DEL/EXISTS/INCR/DECR/APPEND/STRLEN/EXPIRE(W2)。
 */
@Getter
@Setter
@AllArgsConstructor
public class RedisString extends RedisObject {
    private String value;
}
