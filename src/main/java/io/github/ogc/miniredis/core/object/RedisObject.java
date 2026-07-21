package io.github.ogc.miniredis.core.object;

/**
 * Redis 存储对象基类 -- 内存里的数据类型,和 {@link io.github.ogc.miniredis.resp.RespObject}(网络协议类型)区分开。
 *
 * <p>子类:RedisString / RedisHash / RedisList / RedisSet / RedisZSet。
 * W2 先实现 String + Hash,其余每周加一个。
 *
 * <p>为什么用抽象类不用接口:后续(W4)要加 lastAccessTime 字段做 LRU 淘汰,
 * 字段放抽象类里所有子类共享,接口(default 方法)放实例字段别扭。
 */
public abstract class RedisObject {

    /**
     * 过期时间戳(毫秒),-1 表示永不过期。
     * <p>W2 实现 EXPIRE + 惰性删除:访问时由 {@code RedisDb.get()} 检查 isExpired() 并删除。
     * W3 再加后台定期扫描,凑齐 Redis 的"惰性 + 定期"双策略。
     * <p>volatile:当前 Worker 单线程无竞争,但 W3/W5 引入后台线程后保证可见性。
     */
    private volatile long expireAt = -1;

    /** 是否已过期。永不过期(-1)直接返回 false,避免无谓的 currentTimeMillis 调用。 */
    public boolean isExpired() {
        return expireAt != -1 && System.currentTimeMillis() >= expireAt;
    }

    public void setExpireAt(long expireAtMillis) {
        this.expireAt = expireAtMillis;
    }

    public void clearExpire() {
        this.expireAt = -1;
    }

    public long getExpireAt() {
        return expireAt;
    }
}
