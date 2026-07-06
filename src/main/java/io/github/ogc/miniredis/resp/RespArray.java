package io.github.ogc.miniredis.resp;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * RESP 数组,如 *2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
 * <p>元素类型固定为 {@link RespObject},确保编译期类型安全 —— 不再是 List&lt;Object&gt;。
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public final class RespArray implements RespObject {
    private final List<RespObject> elements;

    public int size() {
        return elements.size();
    }

    public RespObject get(int index) {
        return elements.get(index);
    }
}
