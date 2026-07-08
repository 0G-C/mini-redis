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
}
