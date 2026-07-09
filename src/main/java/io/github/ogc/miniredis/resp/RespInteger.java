package io.github.ogc.miniredis.resp;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * RESP 整数,如 {@code :1\r\n}。
 * <p>用于 DEL(返回删除 key 数量)、EXISTS、INCR 等返回整数的命令。
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class RespInteger implements RespObject {
    private final long value;
}
