package io.github.ogc.miniredis.resp;

import lombok.Data;

/**
 * RESP 简单字符串，如 +OK\r\n
 */
@Data
public class RespSimpleString {
    private final String value;

}
