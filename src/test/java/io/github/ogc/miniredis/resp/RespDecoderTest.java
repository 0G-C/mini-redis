package io.github.ogc.miniredis.resp;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RespDecoderTest {

    @Test
    void testSimpleString_normal() {
        // 准备：把 RespDecoder 装进 EmbeddedChannel（模拟 ChannelPipeline）
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        // 模拟收到数据 "+OK\r\n"
        channel.writeInbound(Unpooled.copiedBuffer("+OK\r\n", StandardCharsets.UTF_8));

        // 从 channel 里读出解析结果
        RespSimpleString result = channel.readInbound();

        assertThat(result.getValue()).isEqualTo("OK");
    }

    @Test
    void testSimpleString_halfPacket() {
        // 半包：只收到一半，还没有 \n
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("+OK\r", StandardCharsets.UTF_8));

        // 数据不全，不应该有输出
        RespSimpleString result = channel.readInbound();
        assertThat(result).isNull();

        // 后半部分到了
        channel.writeInbound(Unpooled.copiedBuffer("\n", StandardCharsets.UTF_8));

        // 现在能解析出来了
        result = channel.readInbound();
        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("OK");
    }

    @Test
    void testSimpleString_emptyBuffer() {
        // 空数据不应该崩溃
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.EMPTY_BUFFER);

        RespSimpleString result = channel.readInbound();
        assertThat(result).isNull();
    }

    // ==================== BulkString 测试 ====================

    @Test
    void testBulkString_normal() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("$4\r\nPING\r\n", StandardCharsets.UTF_8));

        RespBulkString result = channel.readInbound();
        assertThat(result.getValue()).isEqualTo("PING");
    }

    @Test
    void testBulkString_null() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("$-1\r\n", StandardCharsets.UTF_8));

        RespBulkString result = channel.readInbound();
        assertThat(result.isNull()).isTrue();
    }

    @Test
    void testBulkString_halfPacket_contentMissing() {
        // $4\r\nPING\r\n 只收到 $4\r\nPIN —— 剩下 G\r\n 还没到
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("$4\r\nPIN", StandardCharsets.UTF_8));

        // 数据不全，无输出
        assertThat(channel.<RespBulkString>readInbound()).isNull();

        // 后半到了
        channel.writeInbound(Unpooled.copiedBuffer("G\r\n", StandardCharsets.UTF_8));

        RespBulkString result = channel.readInbound();
        assertThat(result.getValue()).isEqualTo("PING");
    }

    // ==================== Array 测试 ====================

    @Test
    void testArray_echo() {
        // *2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        String raw = "*2\r\n$4\r\nECHO\r\n$5\r\nhello\r\n";
        channel.writeInbound(Unpooled.copiedBuffer(raw, StandardCharsets.UTF_8));

        RespArray array = channel.readInbound();
        assertThat(array.size()).isEqualTo(2);
        assertThat(((RespBulkString) array.get(0)).getValue()).isEqualTo("ECHO");
        assertThat(((RespBulkString) array.get(1)).getValue()).isEqualTo("hello");
    }

    @Test
    void testArray_ping() {
        // *1\r\n$4\r\nPING\r\n
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("*1\r\n$4\r\nPING\r\n", StandardCharsets.UTF_8));

        RespArray array = channel.readInbound();
        assertThat(array.size()).isEqualTo(1);
        assertThat(((RespBulkString) array.get(0)).getValue()).isEqualTo("PING");
    }
}
