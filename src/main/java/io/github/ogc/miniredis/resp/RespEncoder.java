package io.github.ogc.miniredis.resp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * RESP2 协议编码器:{@link RespObject} → RESP 字节流。
 * <p>支持嵌套 Array,通过 {@link #encodeOne} 递归实现。
 */
public class RespEncoder extends MessageToByteEncoder<RespObject> {

    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] NULL_BULK = "$-1\r\n".getBytes(StandardCharsets.UTF_8);

    @Override
    protected void encode(ChannelHandlerContext ctx, RespObject msg, ByteBuf out) {
        encodeOne(msg, out);
    }

    /**
     * 编码单个对象。Array 内元素递归走此方法,支持嵌套。
     */
    private void encodeOne(RespObject msg, ByteBuf out) {
        if (msg instanceof RespSimpleString s) {
            out.writeByte('+');
            out.writeCharSequence(s.getValue(), StandardCharsets.UTF_8);
            out.writeBytes(CRLF);
        } else if (msg instanceof RespInteger i) {
            out.writeByte(':');
            out.writeCharSequence(String.valueOf(i.getValue()), StandardCharsets.UTF_8);
            out.writeBytes(CRLF);
        } else if (msg instanceof RespBulkString b) {
            if (b.isNull()) {
                out.writeBytes(NULL_BULK);
            } else {
                byte[] bytes = b.getValue().getBytes(StandardCharsets.UTF_8);
                out.writeByte('$');
                out.writeCharSequence(String.valueOf(bytes.length), StandardCharsets.UTF_8);
                out.writeBytes(CRLF);
                out.writeBytes(bytes);
                out.writeBytes(CRLF);
            }
        } else if (msg instanceof RespArray a) {
            out.writeByte('*');
            out.writeCharSequence(String.valueOf(a.size()), StandardCharsets.UTF_8);
            out.writeBytes(CRLF);
            for (RespObject element : a.getElements()) {
                encodeOne(element, out);
            }
        }
        // sealed interface 保证 exhaustive,不需要 else
    }
}
