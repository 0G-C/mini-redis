package io.github.ogc.miniredis.resp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RESP2 协议解码器。
 * <p>支持类型:{@code +SimpleString} / {@code $BulkString} / {@code *Array}(可嵌套)。
 * <p>半包处理策略:
 * <ol>
 *   <li>顶层 {@link #decode} 在读第一个字节前 mark,helper 方法返回 false 表示数据不完整,顶层负责 reset。</li>
 *   <li>Array 内元素也走同样约定:元素 helper 返回 false → Array 整体 reset,等下次一批数据一起重试。</li>
 * </ol>
 * <p>协议违规(未知类型、非法长度)直接抛 {@link RespProtocolException},不做半包处理。
 */
public class RespDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() == 0) return;

        in.markReaderIndex();

        List<RespObject> parsed = new ArrayList<>(1);
        if (!decodeOne(in, parsed)) {
            in.resetReaderIndex();
            return;
        }

        out.addAll(parsed);
    }

    /**
     * 解码一个 RESP 对象。
     * @return true 完整读到一个对象 & 已经写入 out,false 数据不完整需 caller reset
     * @throws RespProtocolException 未知类型或非法长度
     */
    private boolean decodeOne(ByteBuf in, List<RespObject> out) {
        if (in.readableBytes() == 0) return false;

        byte firstByte = in.getByte(in.readerIndex());
        return switch (firstByte) {
            case '+' -> decodeSimpleString(in, out);
            case '$' -> decodeBulkString(in, out);
            case '*' -> decodeArray(in, out);
            case ':' -> decodeInteger(in, out);
            default -> throw new RespProtocolException("unknown type marker: " + (char) firstByte);
        };
    }

    // ==================== +SimpleString ====================

    private boolean decodeSimpleString(ByteBuf in, List<RespObject> out) {
        int lineEnd = in.bytesBefore((byte) '\n');
        if (lineEnd == -1) return false;

        in.readByte(); // 吃掉 '+'
        String content = in.readCharSequence(lineEnd - 2, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过 \r\n
        out.add(new RespSimpleString(content));
        return true;
    }

    // ==================== :Integer ====================

    private boolean decodeInteger(ByteBuf in, List<RespObject> out) {
        int lineEnd = in.bytesBefore((byte) '\n');
        if (lineEnd == -1) return false;

        in.readByte(); // 吃掉 ':'
        String valueStr = in.readCharSequence(lineEnd - 2, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过 \r\n

        long value;
        try {
            value = Long.parseLong(valueStr);
        } catch (NumberFormatException e) {
            throw new RespProtocolException("invalid integer: " + valueStr);
        }
        out.add(new RespInteger(value));
        return true;
    }

    // ==================== $BulkString ====================

    private boolean decodeBulkString(ByteBuf in, List<RespObject> out) {
        int lineEnd = in.bytesBefore((byte) '\n');
        if (lineEnd == -1) return false;

        in.readByte(); // 吃掉 '$'
        String lengthStr = in.readCharSequence(lineEnd - 2, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过长度行的 \r\n

        int contentLen;
        try {
            contentLen = Integer.parseInt(lengthStr);
        } catch (NumberFormatException e) {
            throw new RespProtocolException("invalid bulk string length: " + lengthStr);
        }

        if (contentLen == -1) {
            out.add(RespBulkString.NULL);
            return true;
        }
        if (contentLen < 0) {
            throw new RespProtocolException("invalid bulk string length: " + contentLen);
        }

        if (in.readableBytes() < contentLen + 2) {
            return false; // 内容不完整,caller reset
        }

        String content = in.readCharSequence(contentLen, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过内容后的 \r\n
        out.add(new RespBulkString(content));
        return true;
    }

    // ==================== *Array ====================

    private boolean decodeArray(ByteBuf in, List<RespObject> out) {
        int lineEnd = in.bytesBefore((byte) '\n');
        if (lineEnd == -1) return false;

        in.readByte(); // 吃掉 '*'
        String countStr = in.readCharSequence(lineEnd - 2, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过 \r\n

        int count;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            throw new RespProtocolException("invalid array count: " + countStr);
        }
        if (count < 0) {
            throw new RespProtocolException("invalid array count: " + count);
        }

        List<RespObject> elements = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (!decodeOne(in, elements)) {
                return false; // 任一元素半包,整个 Array reset(由顶层 decode 负责)
            }
        }

        out.add(new RespArray(elements));
        return true;
    }
}
