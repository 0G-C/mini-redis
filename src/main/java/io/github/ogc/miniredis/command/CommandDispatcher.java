package io.github.ogc.miniredis.command;

import io.github.ogc.miniredis.core.RedisDb;
import io.github.ogc.miniredis.resp.RespArray;
import io.github.ogc.miniredis.resp.RespBulkString;
import io.github.ogc.miniredis.resp.RespObject;
import io.github.ogc.miniredis.resp.RespSimpleString;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 命令分发器 -- 全局单例,持有命令注册表,负责把客户端命令路由到对应 {@link Command}。
 *
 * <p>单例理由:命令注册表(扫描 + 实例化命令)只需做一次,所有连接共享这一份,
 * 避免每个连接重复扫描。详见 [[command-dispatcher-singleton]]。
 *
 * <p>生命周期:
 * <ul>
 *   <li>启动时:{@link ClassPathScanner} 扫描 command 包 -> 填充 {@link #commands} map</li>
 *   <li>运行时:每条命令到达 -> {@link #dispatch} 查 map -> 调 execute</li>
 * </ul>
 */
@Slf4j
public class CommandDispatcher {

    private static final CommandDispatcher INSTANCE = new CommandDispatcher();

    /** 命令名(大写) -> Command 实例。启动后只读不写。 */
    private final Map<String, Command> commands;

    private CommandDispatcher() {
        this.commands = ClassPathScanner.scan("io.github.ogc.miniredis.command");
        log.info("CommandDispatcher initialized with {} commands", commands.size());
    }

    public static CommandDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * 分发一条命令。
     *
     * <p>职责:协议层守卫(是 Array / 非空 / 首元素是 BulkString)
     * + 查表 + 调 execute + 兜底异常。
     * CommandHandler 只管网络,把 RespObject 丢给这里就行。
     *
     * @param msg decoder 解出的 RESP 对象
     * @return 响应,由 CommandHandler writeAndFlush 出去
     */
    public RespObject dispatch(RespObject msg) {
        // 守卫 1:命令必是 Array
        if (!(msg instanceof RespArray array)) {
            log.warn("expected RespArray command, got {}", msg.getClass().getSimpleName());
            return new RespSimpleString("ERR protocol error: command must be array");
        }
        // 守卫 2:非空
        if (array.size() == 0) {
            return new RespSimpleString("ERR protocol error: empty command");
        }
        // 守卫 3:首元素是非 null BulkString(命令名)
        if (!(array.get(0) instanceof RespBulkString cmdBulk) || cmdBulk.isNull()) {
            return new RespSimpleString("ERR protocol error: command name must be bulk string");
        }

        String name = cmdBulk.getValue().toUpperCase();
        Command cmd = commands.get(name);
        if (cmd == null) {
            return new RespSimpleString("ERR unknown command '" + name + "'");
        }

        try {
            return cmd.execute(RedisDb.getInstance(), array);
        } catch (Exception e) {
            log.error("command '{}' execution failed", name, e);
            return new RespSimpleString("ERR internal error executing '" + name + "'");
        }
    }
}
