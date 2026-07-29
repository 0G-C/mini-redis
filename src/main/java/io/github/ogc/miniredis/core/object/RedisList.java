package io.github.ogc.miniredis.core.object;

import lombok.Getter;

import java.util.LinkedList;

/**
 * Redis List 类型 —— 双向链表,头尾 O(1)。
 * <p>Java 的 {@link LinkedList} 即双向链表,天然适合 List 的 LPUSH/RPUSH/LPOP/RPOP。
 * <p>Redis 官方用 quicklist(ziplist + linkedlist 混合),这里是简化版。
 */
@Getter
public class RedisList extends RedisObject {
    private final LinkedList<String> elements = new LinkedList<>();
}
