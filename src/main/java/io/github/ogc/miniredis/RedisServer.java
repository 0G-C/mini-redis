package io.github.ogc.miniredis;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedisServer {

    private final int port;

    public RedisServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        // 开门组：1 个线程，只做 accept
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 聊天组：1 个线程（和 Redis 一样，单线程处理所有命令）
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                     .channel(NioServerSocketChannel.class)
                     .childHandler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             log.info("新连接进来: {}", ch.remoteAddress());
                         }
                     });

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("mini-redis 启动成功，监听端口: {}", port);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new RedisServer(6380).start();
    }
}
