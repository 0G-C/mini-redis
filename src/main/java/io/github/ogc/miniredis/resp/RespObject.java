package io.github.ogc.miniredis.resp;

/**
 * RESP 协议所有类型的顶层接口。
 * <p>使用 Java 17 的 sealed interface,编译期锁定实现类型,方便面试讲解:
 * "我用 sealed interface + pattern matching 让编码器/解码器的类型分派编译期可查"。
 */
public sealed interface RespObject
        permits RespSimpleString, RespBulkString, RespArray, RespInteger {
}
