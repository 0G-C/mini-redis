package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespObject;

/**
 * 命令接口 -- 所有 Redis 命令实现此接口。
 *
 * <p>命令通过 {@link CommandName} 注解登记自己的名字(如 "GET"),
 * 启动时由 {@link ClassPathScanner} 扫描 command 包自动发现并注册到 {@link CommandDispatcher}。
 * 加新命令只需写一个类 + 贴注解,不用改任何注册代码。
 *
 * <p>参数约定:{@code args} 是完整的 RESP 数组,{@code args.get(0)} 是命令名本身,
 * {@code args.get(1)} 起是真正参数(和 Redis 官方 argv 一致)。
 *
 * <p>为什么 args 包含命令名:和 RESP 原始结构一致,避免 dispatcher 还要截数组;
 * 命令内部从 index 1 取参数,和 Redis 源码的 argv[0] 约定对齐。
 */
public interface Command {

    /**
     * 执行命令。
     *
     * @param db   全局存储(单例),命令读写数据用
     * @param args 完整命令数组,args[0]=命令名,args[1+]=参数
     * @return RESP 响应对象,由 encoder 编回字节流发给客户端
     */
    RespObject execute(RedisDb db, RespArray args);
}
