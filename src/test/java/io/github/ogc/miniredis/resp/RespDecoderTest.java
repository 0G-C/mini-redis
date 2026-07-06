package io.github.ogc.miniredis.resp;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RespDecoderTest {

    @Test
    void testSimpleString_normal() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("+OK\r\n", StandardCharsets.UTF_8));

        RespSimpleString result = channel.readInbound();
        assertThat(result.getValue()).isEqualTo("OK");
    }

    @Test
    void testSimpleString_halfPacket() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("+OK\r", StandardCharsets.UTF_8));

        RespSimpleString result = channel.readInbound();
        assertThat(result).isNull();

        channel.writeInbound(Unpooled.copiedBuffer("\n", StandardCharsets.UTF_8));

        result = channel.readInbound();
        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("OK");
    }

    @Test
    void testSimpleString_emptyBuffer() {
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
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("$4\r\nPIN", StandardCharsets.UTF_8));
        assertThat(channel.<RespBulkString>readInbound()).isNull();

        channel.writeInbound(Unpooled.copiedBuffer("G\r\n", StandardCharsets.UTF_8));

        RespBulkString result = channel.readInbound();
        assertThat(result.getValue()).isEqualTo("PING");
    }

    // ==================== Array 测试 ====================

    @Test
    void testArray_echo() {
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
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("*1\r\n$4\r\nPING\r\n", StandardCharsets.UTF_8));

        RespArray array = channel.readInbound();
        assertThat(array.size()).isEqualTo(1);
        assertThat(((RespBulkString) array.get(0)).getValue()).isEqualTo("PING");
    }

    // ==================== 新增:补齐边界与异常 ====================

    /**
     * 粘包:一次 buffer 里同时到两条完整消息,应分两次解出。
     */
    @Test
    void testMultipleMessages_inOneBuffer() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        channel.writeInbound(Unpooled.copiedBuffer("+OK\r\n+PONG\r\n", StandardCharsets.UTF_8));

        RespSimpleString first = channel.readInbound();
        RespSimpleString second = channel.readInbound();
        assertThat(first.getValue()).isEqualTo("OK");
        assertThat(second.getValue()).isEqualTo("PONG");
    }

    /**
     * Array 半包:头部到了但某个 bulk string 内容不全,应等下一批。
     */
    @Test
    void testArray_halfPacket_atMiddleElement() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        // *2\r\n$3\r\nfoo\r\n$5\r\nhello\r\n 拆两次
        channel.writeInbound(Unpooled.copiedBuffer("*2\r\n$3\r\nfoo\r\n$5\r\nhel", StandardCharsets.UTF_8));
        assertThat(channel.<RespArray>readInbound()).isNull();

        channel.writeInbound(Unpooled.copiedBuffer("lo\r\n", StandardCharsets.UTF_8));

        RespArray array = channel.readInbound();
        assertThat(array.size()).isEqualTo(2);
        assertThat(((RespBulkString) array.get(0)).getValue()).isEqualTo("foo");
        assertThat(((RespBulkString) array.get(1)).getValue()).isEqualTo("hello");
    }

    /**
     * Array 内嵌套 SimpleString 半包:回归 Bug 1 场景。
     */
    @Test
    void testArray_withSimpleString_halfPacket() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        // *2\r\n+OK\r\n$3\r\nfoo\r\n,第一次 SimpleString 半包
        channel.writeInbound(Unpooled.copiedBuffer("*2\r\n+O", StandardCharsets.UTF_8));
        assertThat(channel.<RespArray>readInbound()).isNull();

        channel.writeInbound(Unpooled.copiedBuffer("K\r\n$3\r\nfoo\r\n", StandardCharsets.UTF_8));

        RespArray array = channel.readInbound();
        assertThat(array.size()).isEqualTo(2);
        assertThat(((RespSimpleString) array.get(0)).getValue()).isEqualTo("OK");
        assertThat(((RespBulkString) array.get(1)).getValue()).isEqualTo("foo");
    }

    /**
     * 非法 bulk 长度:应抛协议异常,不静默吞掉。
     */
    @Test
    void testBulkString_invalidLength_throws() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        // Netty 把 handler 里的异常包装成 DecoderException,原因是 RespProtocolException
        assertThatThrownBy(() ->
                channel.writeInbound(Unpooled.copiedBuffer("$abc\r\n", StandardCharsets.UTF_8)))
                .hasRootCauseInstanceOf(RespProtocolException.class);
    }

    /**
     * 未知类型标记:抛协议异常。
     */
    @Test
    void testUnknownTypeMarker_throws() {
        EmbeddedChannel channel = new EmbeddedChannel(new RespDecoder());

        assertThatThrownBy(() ->
                channel.writeInbound(Unpooled.copiedBuffer("#weird\r\n", StandardCharsets.UTF_8)))
                .hasRootCauseInstanceOf(RespProtocolException.class);
    }
}
