package io.github.ogc.miniredis.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 命令名注解 -- 贴在 {@link Command} 实现类上,声明这个类处理哪个命令。
 *
 * <p>例:{@code @CommandName("GET")} 贴在 GetCommand 上,
 * 启动时扫描器读到这个注解,把 "GET" -> GetCommand实例 存进 dispatcher 的 map。
 *
 * <p>{@code @Retention(RUNTIME)} 是关键:注解必须保留到运行时,
 * 否则编译后就被擦除,扫描器的 {@code getAnnotation} 拿不到。这是注解扫描最常见的坑。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandName {
    /** 命令名,大写,如 "GET" / "SET" / "PING" */
    String value();
}
