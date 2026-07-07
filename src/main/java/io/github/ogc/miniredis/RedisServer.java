package io.github.ogc.miniredis;

import io.github.ogc.miniredis.handler.CommandHandler;
import io.github.ogc.miniredis.resp.RespDecoder;
import io.github.ogc.miniredis.resp.RespEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * Mini-Redis 服务器入口。
 *
 * <p>网络模型:Netty Reactor,Boss(1) 负责 accept,Worker(1) 处理所有 IO 和命令 ——
 * 和 Redis 一样单线程执行命令,避免并发锁。
 *
 * <p>Pipeline:
 * <pre>
 *   inbound:  bytes → [RespDecoder] → RespObject → [CommandHandler] → 处理
 *   outbound: RespObject → [RespEncoder] → bytes → 网络
 * </pre>
 *
 * <p>启动方式:{@code java RedisServer [port]},默认端口 6380(避开官方 Redis 的 6379)。
 */
@Slf4j
public class RedisServer {

    private static final int DEFAULT_PORT = 6380;

    private final int port;

    public RedisServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        // 开门组:1 个线程,只做 accept
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 聊天组:1 个线程(和 Redis 一样,单线程处理所有命令)
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // SO_BACKLOG:accept 队列长度,连接尚未 accept 时排队上限
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // TCP_NODELAY:关闭 Nagle 算法,小包立即发送(Redis 场景延迟敏感)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // SO_KEEPALIVE:开启 TCP 心跳,长时间空闲的死连接会被系统层清理
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            log.info("新连接进来: {}", ch.remoteAddress());
                            ch.pipeline()
                                    .addLast("decoder", new RespDecoder())
                                    .addLast("encoder", new RespEncoder())
                                    .addLast("handler", new CommandHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("mini-redis 启动成功,监听端口: {}", port);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new RedisServer(port).start();
    }
}
