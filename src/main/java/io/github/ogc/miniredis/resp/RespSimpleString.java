package io.github.ogc.miniredis.resp;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * RESP 简单字符串,如 +OK\r\n
 * <p>用 {@code @Getter + @AllArgsConstructor + @EqualsAndHashCode + @ToString} 而不是 {@code @Data},
 * 因为 {@code @Data} 在 final 字段场景下语义模糊:setter 生成不了、无参构造器也不完整。
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class RespSimpleString implements RespObject {
    private final String value;
}
