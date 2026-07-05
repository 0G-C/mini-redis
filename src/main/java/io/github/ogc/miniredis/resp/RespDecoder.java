package io.github.ogc.miniredis.resp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RESP2 协议解码器。
 * 支持类型: +SimpleString / $BulkString / *Array
 */
public class RespDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() == 0) return;

        byte firstByte = in.getByte(in.readerIndex());

        if (firstByte == '+') {
            decodeSimpleString(in, out);
        } else if (firstByte == '$') {
            in.markReaderIndex();
            if (!decodeBulkString(in, out)) {
                in.resetReaderIndex();
            }
        } else if (firstByte == '*') {
            decodeArray(in, out);
        }
    }

    // ==================== +SimpleString ====================

    private void decodeSimpleString(ByteBuf in, List<Object> out) {
        int lineEnd = in.bytesBefore((byte) '\n');
        if (lineEnd == -1) return;

        in.readByte(); // 吃掉 '+'
        String content = in.readCharSequence(lineEnd - 2, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过 \r\n
        out.add(new RespSimpleString(content));
    }

    // ==================== $BulkString ====================

    /**
     * 解析一个 BulkString。调用前 caller 应做好 mark。
     * @return true 解析成功，false 数据不完整需等下次
     */
    private boolean decodeBulkString(ByteBuf in, List<Object> out) {
        int lineEnd = in.bytesBefore((byte) '\n');
        if (lineEnd == -1) return false;

        in.readByte(); // 吃掉 '$'
        String lengthStr = in.readCharSequence(lineEnd - 2, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过长度行的 \r\n

        int contentLen = Integer.parseInt(lengthStr);
        if (contentLen == -1) {
            out.add(RespBulkString.NULL);
            return true;
        }

        if (in.readableBytes() < contentLen + 2) {
            return false; // 内容不完整，caller 会 reset
        }

        String content = in.readCharSequence(contentLen, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过内容后的 \r\n
        out.add(new RespBulkString(content));
        return true;
    }

    // ==================== *Array ====================

    private void decodeArray(ByteBuf in, List<Object> out) {
        in.markReaderIndex();

        int lineEnd = in.bytesBefore((byte) '\n');
        if (lineEnd == -1) return;

        in.readByte(); // 吃掉 '*'
        String countStr = in.readCharSequence(lineEnd - 2, StandardCharsets.UTF_8).toString();
        in.skipBytes(2); // 跳过 \r\n

        int count = Integer.parseInt(countStr);
        List<Object> elements = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            if (in.readableBytes() == 0) {
                in.resetReaderIndex();
                return;
            }

            byte elementType = in.getByte(in.readerIndex());

            if (elementType == '$') {
                if (!decodeBulkString(in, elements)) {
                    in.resetReaderIndex();
                    return;
                }
            } else if (elementType == '+') {
                decodeSimpleString(in, elements);
            }
            // 其他类型 (- : *) 后续补充
        }

        out.add(new RespArray(elements));
    }
}
