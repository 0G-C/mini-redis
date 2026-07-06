package io.github.ogc.miniredis.resp;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * RESP 批量字符串,如 $5\r\nhello\r\n
 * <p>null 值用 $-1\r\n 表示,对应 {@link #NULL} 常量。
 * <p>调用 {@link #getValue()} 前应先调 {@link #isNull()} 检查,否则可能 NPE。
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class RespBulkString implements RespObject {
    public static final RespBulkString NULL = new RespBulkString(null);

    private final String value;

    public boolean isNull() {
        return value == null;
    }
}
