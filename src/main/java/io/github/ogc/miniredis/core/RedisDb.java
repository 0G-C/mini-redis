package io.github.ogc.miniredis.core;

import io.github.ogc.miniredis.core.object.RedisObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 存储层 -- 全局单例,核心是一个 dict(键值表)。
 *
 * <p>单例理由:和 CommandDispatcher 一样,存储全局只有一份,所有连接共享。
 *
 * <p>键类型讨论:这里用 {@code String} 做 key。
 * Redis 官方用 sds(二进制安全),可存任意字节。
 * Java 里 {@code byte[]} 做 HashMap key 有坑(equals/hashCode 是引用比较,不是内容比较),
 * 要二进制安全得引入包装类。W2 先用 String 跑通,二进制安全留作 TODO 升级。
 *
 * <p>线程安全:用 {@link ConcurrentHashMap}。当前 Worker 单线程跑命令,实际无竞争;
 * 但持久化(W5)、过期扫描(W3)会引入后台线程,提前用并发容器更稳。
 */
public class RedisDb {

    private static final RedisDb INSTANCE = new RedisDb();

    private final ConcurrentHashMap<String, RedisObject> dict = new ConcurrentHashMap<>();

    private RedisDb() {
    }

    public static RedisDb getInstance() {
        return INSTANCE;
    }

    public RedisObject get(String key) {
        RedisObject obj = dict.get(key);
        if (obj != null && obj.isExpired()) {
            // 惰性删除:访问到过期 key 时顺手删掉。
            // remove(key, obj) 用引用比较条件删除,不会误删并发替换后的新值。
            dict.remove(key, obj);
            return null;
        }
        return obj;
    }

    public void put(String key, RedisObject value) {
        dict.put(key, value);
    }

    public RedisObject remove(String key) {
        return dict.remove(key);
    }

    public boolean exists(String key) {
        return dict.containsKey(key);
    }

    public int size() {
        return dict.size();
    }

    /** 清空所有键值(仅测试用)。 */
    public void clear() {
        dict.clear();
    }
}
