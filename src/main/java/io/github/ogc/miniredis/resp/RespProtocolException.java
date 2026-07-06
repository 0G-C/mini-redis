package io.github.ogc.miniredis.resp;

/**
 * RESP 协议解析异常。
 * <p>抛出场景:未知类型标记、非法长度(如负数、非数字)等协议违规情况。
 * 与半包(数据不完整)区分:半包返回 false 由 caller reset,协议错则直接抛异常。
 */
public class RespProtocolException extends RuntimeException {
    public RespProtocolException(String message) {
        super(message);
    }
}
