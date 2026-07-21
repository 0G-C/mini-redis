# 进度总览

> **AI 助手接手时,只需要读本文件 + `PLAN.md`。**
> 需要历史细节时再按需读对应 `PROGRESS/Wn.md`。
> 维护规则见 `PLAN.md` 附录 C(用户不手动维护本目录,AI 自主维护)。

---

## 当前状态

- **今日**:2026-07-21(周二)@ 公司 Windows
- **阶段**:W2 String + Hash + 命令分发(7/11-7/17,延期至 7/21 完成)
- **进行中**:W2 全部完成 —— 16 命令(APPEND/COMMAND/DECR/DEL/ECHO/EXPIRE/GET/HDEL/HGET/HGETALL/HLEN/HSET/INCR/PING/SET/STRLEN)注册,52 测试全绿
- **卡壳**:无
- **下一步**:W3 List + Set + 过期定期扫描

---

## 阶段完成度

- [x] **W0 环境搭建**(2026-07-03 完成)-- 详见 [W0.md](W0.md)
- [x] **W1 骨架 + RESP 协议**(7/3-7/7 主线完成,提前 3 天)-- 见 [W1.md](W1.md);复盘:用户判断内容量不值得专门做,跳过
- [x] **W2 String + Hash + 命令分发**(7/11-7/17,延期至 7/21 完成)-- 见 [W2.md](W2.md)
- [ ] **W3 List + Set + 过期机制**(7/18-7/24)
- [ ] **W4 ZSet + SkipList + LRU 淘汰**(7/25-7/31)
- [ ] **W5 AOF + 简化 RDB**(8/1-8/7)
- [ ] **W6 JMH 压测 + 文档 + 简历**(8/8-8/14)

---

## 面试可讲的卡壳->解决故事(累积中)

> AI 归档周日志时,把有面试价值的一条抽到这里。W6 简历定稿时挑最好的 2-3 个用。

- **RESP 半包处理的契约演化**(W1)-- 最初 `decodeSimpleString` 只有 `$` 型返回半包信号,`+` 型半包时静默继续,导致 Array 内嵌套 SimpleString 半包会状态错乱。修复时统一契约:所有 helper 返回 boolean,顶层单次 mark/reset,任一 helper 说 false 就全局 reset。配套引入 `decodeOne` 递归分派,自然支持 nested Array。**面试角度**:"状态机契约设计"、"一次 mark/reset 覆盖多层结构"、"从边界 case 反推架构缺陷"。
- **Netty 异常传播的隐藏包装**(W1)-- decoder 抛的 `RespProtocolException` 被 Netty 包装成 `DecoderException`,测试用 `assertThatThrownBy().isInstanceOf(RespProtocolException.class)` 会失败,必须用 `hasRootCauseInstanceOf`。**面试角度**:"Netty pipeline 异常传播机制"、"AssertJ 三种异常断言的差异"、"库封装边界如何影响测试"。
- **Git pull 错误合并到 main 的救援**(W1 阶段)-- 居家在 main 分支上误 `git pull origin journal`,产生 merge commit 混入 doc 内容。因为没 push,用 `git reset --hard origin/main` 干净回滚。**面试角度**:"reset vs revert 的选择"、"公共分支只能 revert,私人未 push 用 reset"、"pull 默认 merge 的坑,推荐 `pull.rebase true`"。
- **命令分发的工程化选型**(W2)-- AI 建议手动注册(30 行 Map.put"简单够用")被用户否决:学习项目要走工程化。最终选"反射+注解+类路径扫描",手写 60 行扫描器处理 file/jar 双 classpath 协议,加命令只写一个类贴 `@CommandName`。对比 SPI(杀鸡用牛刀)。**面试角度**:"类路径扫描原理"、"注解运行时保留机制(`@Retention RUNTIME`)为什么是命门"、"Spring ComponentScan 精简版"、"工程化 vs 简化的取舍"。
