package io.github.ogc.miniredis.handler;

import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandHandlerTest {

    @Test
    void ping_noArg_returnsPong() {
        EmbeddedChannel channel = new EmbeddedChannel(new CommandHandler());

        // *1\r\n$4\r\nPING\r\n
        channel.writeInbound(new RespArray(List.of(new RespBulkString("PING"))));

        RespObject response = channel.readOutbound();
        assertThat(response).isInstanceOf(RespSimpleString.class);
        assertThat(((RespSimpleString) response).getValue()).isEqualTo("PONG");
    }

    @Test
    void ping_withArg_returnsArg() {
        EmbeddedChannel channel = new EmbeddedChannel(new CommandHandler());

        channel.writeInbound(new RespArray(List.of(
                new RespBulkString("PING"),
                new RespBulkString("hello"))));

        RespObject response = channel.readOutbound();
        assertThat(response).isInstanceOf(RespBulkString.class);
        assertThat(((RespBulkString) response).getValue()).isEqualTo("hello");
    }

    @Test
    void echo_returnsArg() {
        EmbeddedChannel channel = new EmbeddedChannel(new CommandHandler());

        channel.writeInbound(new RespArray(List.of(
                new RespBulkString("ECHO"),
                new RespBulkString("world"))));

        RespObject response = channel.readOutbound();
        assertThat(response).isInstanceOf(RespBulkString.class);
        assertThat(((RespBulkString) response).getValue()).isEqualTo("world");
    }

    @Test
    void commandName_isCaseInsensitive() {
        EmbeddedChannel channel = new EmbeddedChannel(new CommandHandler());

        channel.writeInbound(new RespArray(List.of(new RespBulkString("ping"))));

        RespObject response = channel.readOutbound();
        assertThat(((RespSimpleString) response).getValue()).isEqualTo("PONG");
    }

    @Test
    void unknownCommand_returnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(new CommandHandler());

        channel.writeInbound(new RespArray(List.of(new RespBulkString("WEIRD"))));

        RespObject response = channel.readOutbound();
        assertThat(response).isInstanceOf(RespSimpleString.class);
        assertThat(((RespSimpleString) response).getValue()).startsWith("ERR unknown command");
    }

    @Test
    void echo_wrongArgCount_returnsError() {
        EmbeddedChannel channel = new EmbeddedChannel(new CommandHandler());

        // ECHO 没有参数
        channel.writeInbound(new RespArray(List.of(new RespBulkString("ECHO"))));

        RespObject response = channel.readOutbound();
        assertThat(((RespSimpleString) response).getValue()).startsWith("ERR wrong number of arguments");
    }
}
