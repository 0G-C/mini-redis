package io.github.ogc.miniredis.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RESP 批量字符串，如 $5\r\nhello\r\n
 * null 值用 $-1\r\n 表示，对应 RespBulkString.NULL
 */
@Getter
@AllArgsConstructor
public class RespBulkString {
    public static final RespBulkString NULL = new RespBulkString(null);

    private final String value;

    public boolean isNull() {
        return value == null;
    }
}
