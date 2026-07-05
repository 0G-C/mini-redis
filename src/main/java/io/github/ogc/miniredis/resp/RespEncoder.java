package io.github.ogc.miniredis.resp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * RESP2 协议编码器：Java 对象 → RESP 字节流
 */
public class RespEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        // TODO: 你来写

        // if (msg instanceof RespSimpleString s) {
        //     写 '+' → 写内容 → 写 \r\n
        // }
        // else if (msg instanceof RespBulkString b) {
        //     if (b.isNull()) 写 "$-1\r\n"
        //     else 写 '$' → 写长度 → 写 \r\n → 写内容 → 写 \r\n
        // }
    }
}
