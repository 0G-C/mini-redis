package io.github.ogc.miniredis.resp;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * RESP 数组，如 *2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
 */
@Getter
@AllArgsConstructor
public class RespArray {
    private final List<Object> elements;

    public int size() {
        return elements.size();
    }

    public Object get(int index) {
        return elements.get(index);
    }
}
