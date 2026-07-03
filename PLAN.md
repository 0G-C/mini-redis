# Mini-Redis 秋招项目 Plan(自包含版本)

> **文档目标**:任何 AI 助手(Claude/DeepSeek/GPT)读完这份文档,不需要历史对话就能接续辅导。
> 每次和 AI 开新对话时,把这份文档整个贴过去当 prompt 前缀,再问你的具体问题。
>
> **配套文档**:同分支下 `PROGRESS/INDEX.md` 是当前进度快照,AI 每次接手请先读它。历史周日志在 `PROGRESS/Wn.md`,按需读取。
> **维护规则**:用户不手动维护 `PROGRESS/` 目录,由 AI 自主维护 —— 详见附录 C。

---

## 0. 项目背景与约束(给 AI 读的)

**用户身份**
- 川大软工大三,ACM 区域赛铜牌,2026 秋招后端方向
- 携程框架架构部 Redis 组实习中(XPipe / Beacon / CRedis 生态)
- 已合入 ctripcorp/x-pipe 主干 PR #1053(AZ 链路补全,35 文件,+932 -82)
- 主技术栈:Java + Spring 生态
- **自评:项目实操经验薄弱,以往 99% 编码依赖 AI**,现在明确要通过本项目内化技术栈

**项目定位**
- 秋招简历主项目
- Deadline:**2026 年 8 月中**必须完成 MVP(投递期起点)
- 每周投入:22-26 小时(工作日 2h × 5 + 双休 12-16h)
- 总时长:**6 周,7/3 - 8/14**

**技术底座(已决策,不再讨论)**
- 语言:Java 17(可用 record,数据类倾向用 Lombok `@Data` / `@Slf4j`)
- 网络:Netty 4.1.108.Final
- 构建:Maven(已定,pom.xml 在 main 分支)
- 测试:JUnit 5 + Mockito + AssertJ
- 日志:SLF4J + Logback(已配置,`src/main/resources/logback.xml`)
- 压测:JMH 1.37
- 参考实现:
  - https://github.com/wiqer/ef-redis(Java Mini-Redis)
  - https://github.com/tokio-rs/mini-redis(Rust,读它的分层结构)
  - Redis 6.2.6 官方源码(用户已编译过)

**仓库分支策略**
- **`main`**:纯代码分支,commit history 只有功能迭代,面试官看的默认分支
- **`journal`**:orphan 分支,和 main 完全隔离,存放 `PLAN.md` / `PROGRESS.md` / 每日流水账
- 切换命令:`git checkout journal` / `git checkout main`

**开发环境**
- 主开发机:居家 Windows(晚上/周末)+ 公司 Windows(平日上班)
- Mac 只用于公司实习任务,不动 mini-redis
- 官方 Redis 6.2.6 对照:居家 WSL2 里跑,监听默认 6379
- **Mini-Redis 监听 6380**,避开官方端口,方便同机对照压测

**AI 使用政策(重要 —— 给 AI 助手看的)**
- 用户会用 AI 辅助编码,但**每模块**结束时必须做"3 分钟离线自述"
- **允许 AI 生成的部分**:数据类型的具体命令实现、单元测试、JMH 脚本、部署文档、debug 排错、测试辅助工具(如 `Clock` 抽象)
- **必须用户亲手写的部分**(AI 只能答疑,不能给整段代码):
  - RESP 协议编解码(粘包/半包状态机)
  - Netty ChannelHandler 主循环 + EventLoop 组织
  - 主从复制协议(PSYNC / disk-less)
  - AOF 重写触发时机 + BGSAVE 后台快照的多线程协调
- 当 AI 助手识别到用户在"必须亲手写的部分"求整段代码时,应**拒绝直接给完整实现**,改为:引导思路 → 让用户写第一版 → review 用户的版本 → 指出问题
- 用户可以用 AI 讲原理、画图、debug,但**代码本身要用户按下键盘**

---

## 1. 项目最终形态(MVP 定义)

写完 MVP 时,项目应该:

**功能**
- 单机 TCP 服务器,**监听 :6380**(避开官方 Redis 6379,可配置)
- 支持 RESP2 协议(RESP3 不做)
- 5 种数据类型 × 命令(共 ~30 个命令):
  - String:GET/SET/DEL/EXISTS/INCR/DECR/APPEND/STRLEN/EXPIRE
  - List:LPUSH/RPUSH/LPOP/RPOP/LRANGE/LLEN
  - Hash:HSET/HGET/HDEL/HGETALL/HLEN
  - Set:SADD/SREM/SMEMBERS/SISMEMBER
  - ZSet(基于 SkipList):ZADD/ZRANGE/ZRANGEBYSCORE/ZSCORE/ZREM
- 过期机制:惰性删除 + 定期扫描(20ms 一轮,采样 20 个 key)
- 淘汰策略:allkeys-lru(采样 5 个,近似 LRU)
- 持久化:AOF(everysec 策略)+ 简化版 RDB(单线程 snapshot,不 fork)
- 客户端兼容:能用 `redis-cli -p 6380` 直接连并跑通所有命令

**质量**
- 单元测试覆盖率 > 60%(核心模块 > 80%)
- JMH 压测报告:SET/GET 单线程 QPS 对比官方 Redis 6.2.6(目标达到 50%+)
- 代码总量估算:5000-7000 行 Java(不含测试)

**拓展项(有时间做,不阻塞 MVP)**
- 主从复制(PSYNC + disk-less)—— 这个做完面试卡片强度直接翻倍,专门列在 W7-W10
- 类 XPipe Keeper(伪装 Slave 拉数据)—— 顶级拓展,给转正后 9-10 月做

---

## 2. 六周排期总览(相对周数 W1-W6,起点 2026-07-03)

| 周 | 主题 | 交付物 | 关键学习 |
|---|---|---|---|
| **W1** (7/3-7/10) | 骨架 + RESP 协议 | 能用 redis-cli 连上,响应 PONG + ECHO | Netty Reactor / RESP2 解析 |
| **W2** (7/11-7/17) | String + Hash + 命令分发 | 15 个命令可用,单测覆盖 | 单线程模型 / Dispatcher 设计 |
| **W3** (7/18-7/24) | List + Set + 过期机制 | 25 个命令可用,EXPIRE 生效 | 惰性 + 定期扫描 / 时间轮 vs 定期扫描 |
| **W4** (7/25-7/31) | ZSet + SkipList + LRU 淘汰 | 30 个命令齐,内存达 maxmemory 会淘汰 | 跳表实现 / 采样 LRU 算法 |
| **W5** (8/1-8/7) | AOF + 简化 RDB | 重启后数据不丢,AOF 重写触发 | 追加写 / 后台快照 / fsync 策略 |
| **W6** (8/8-8/14) | JMH 压测 + 文档 + 简历 | 压测报告 + README + 简历一句话 | 性能测试方法论 |

**每周内的日节奏建议**
- 工作日(周一至周五):晚上 2h,主要用于阅读参考实现 + 写小段代码
- 周六:6-8h,当周核心模块的主体实现
- 周日:6-8h,写测试 + 3 分钟离线自述 + 卡壳补齐

---

## 3. 每周详细任务清单

### W1(7/3 - 7/10):骨架 + RESP 协议

**目标**:能用 `redis-cli -p 6380 PING` 得到 PONG

**任务清单**(按顺序做)

1. **[环境]** ✅ 已完成(7/3):Maven 工程 + Netty + Lombok + Logback + JUnit/AssertJ/Mockito + JMH,骨架 RedisServer 能启动日志
2. **[骨架]** `RedisServer` 类扩展:main 方法 → Netty ServerBootstrap 启动 :6380 —— *AI 辅助,但你要能画出 boss/worker EventLoopGroup 的关系图*
3. **[协议 - 亲手]** `RespDecoder` 继承 `ByteToMessageDecoder`,实现 RESP2 五种类型的解析:
   - `+SimpleString\r\n`
   - `-Error\r\n`
   - `:Integer\r\n`
   - `$Length\r\nBulkString\r\n`
   - `*Count\r\n...` Array
   - **难点:粘包/半包时 ByteBuf 状态保留**(读一半来了新数据要能续)
4. **[协议 - 亲手]** `RespEncoder` 继承 `MessageToByteEncoder`,把 Java 对象编回 RESP 字节流
5. **[分发]** `CommandHandler` 继承 `SimpleChannelInboundHandler<RespCommand>`,先只实现 PING/ECHO —— *AI 辅助*
6. **[测试]** 用 `redis-cli` 手工验证 PING/ECHO;写 3 个单测 for RespDecoder 的粘包场景 —— *AI 辅助*

**W1 结束的离线自述题**(必须能答)
1. RESP2 协议里 Bulk String 和 Simple String 有什么区别?为什么要有两种?
2. 你的 RespDecoder 里有几个状态?当 TCP 只收到 `$5\r\nhe` 时你的 decoder 会做什么?
3. Netty 的 boss group 和 worker group 分别负责什么?为什么要分开?
4. 如果我让你把 RespDecoder 从头默写一遍,写不出来的地方在哪?

**W1 卡壳时问 AI 的方式(示例)**
- ❌ 错:"帮我写一个 RESP 协议的 decoder"
- ✅ 对:"我在写 RESP2 decoder,现在遇到 Bulk String 半包场景。当 ByteBuf 里只有 `$5\r\nhe` 时,我该怎么保留状态等下一批数据?给我思路,不要给代码"

---

### W2(7/11 - 7/17):String + Hash + 命令分发

**目标**:15 个命令齐全,redis-cli 可以 SET/GET/HSET/HGETALL

**任务清单**

1. **[数据结构]** `RedisDb` 类:核心是一个 `ConcurrentHashMap<byte[], RedisObject>`
   - 讨论点:为什么用 `byte[]` 不用 `String`?(答:兼容二进制安全)
2. **[对象]** `RedisObject` 抽象类 + 子类 `RedisString / RedisHash / RedisList / RedisSet / RedisZSet` —— *AI 辅助*
3. **[分发 - 亲手]** `CommandDispatcher`:
   - Map<String, Command> 注册表
   - 每个 Command 实现 `execute(RedisDb, args) -> RespObject`
   - 讨论点:命令注册用反射 / SPI / 手动 register?
4. **[命令]** 实现 String 类型 8 个命令 —— *前 3 个亲手,后 5 个可 AI 生成再 review*
5. **[命令]** 实现 Hash 类型 5 个命令 —— *AI 辅助*
6. **[单测]** 每个命令至少 3 个单测(正常 / 边界 / 错误参数) —— *AI 生成*

**W2 结束的离线自述题**
1. Redis 是单线程,你的实现是不是也单线程?你的 EventLoop 有几个线程?数据竞争怎么避免?
2. `SET key value EX 60` 里的 EX 是怎么在你的 String 命令里被解析和存储的?
3. 如果一个命令执行到一半抛异常,你怎么保证 client 收到错误响应而不是连接挂掉?
4. Redis 6.0 引入的"多线程 IO"和"单线程执行"分别指什么?你的实现处于哪个模型?

**W2 关键陷阱**
- 别把命令分发写成 if-else 大 switch。用注册表模式。
- 单元测试**不要**用真实 socket,用 EmbeddedChannel(Netty 提供的测试工具)。

---

### W3(7/18 - 7/24):List + Set + 过期机制

**目标**:命令数达 25 个,SET key value EX 60 后 60s 会消失

**任务清单**

1. **[命令]** List:LPUSH/RPUSH/LPOP/RPOP/LRANGE/LLEN —— *可 AI,但内部结构选 LinkedList vs ArrayDeque 要能讲清*
2. **[命令]** Set:SADD/SREM/SMEMBERS/SISMEMBER —— *可 AI*
3. **[过期 - 亲手]** 两套机制并行:
   - **惰性删除**:每次 GET/SET 前先检查是否过期,过期则删
   - **定期扫描**:后台线程每 100ms 唤醒一次,随机采样 20 个 key,过期率超 25% 就再采一批
   - 讨论点:为什么用采样而不是全扫?(答:全扫 STW)
4. **[单测辅助 - AI 辅助]** `Clock` 抽象注入时间(生产是 `System.currentTimeMillis`,测试是 mock),这样测过期不用真的 sleep

**W3 结束的离线自述题**
1. 只用惰性删除会有什么问题?只用定期扫描会有什么问题?为什么 Redis 两个都用?
2. 你的定期扫描每次扫多少个 key?为什么不多不少?
3. 一个 key 已经过期但还没被删,GET 它会返回什么?你的实现里这个判断在哪里?
4. Redis 里"过期时间"是存在哪里的?你的实现里存在哪里?

---

### W4(7/25 - 7/31):ZSet + SkipList + LRU 淘汰

**目标**:30 个命令齐;设置 maxmemory 128mb,写超时会淘汰旧 key

**任务清单**

1. **[数据结构 - 亲手]** SkipList 手写实现
   - `SkipListNode` 有 forward 数组 + score + member
   - `insert / delete / getByRank / getByScoreRange`
   - 讨论点:为什么用 SkipList 不用红黑树?(Redis 作者 antirez 的原文答:实现简单 + 范围查询友好 + 内存局部性)
2. **[命令]** ZSet:ZADD/ZRANGE/ZRANGEBYSCORE/ZSCORE/ZREM
   - Redis 真实实现是 skiplist + hash 双结构,你也要做
3. **[LRU - 亲手]** 采样近似 LRU
   - `RedisObject` 里加 `lastAccessTime` 字段
   - 达 maxmemory 时,随机采 5 个 key,淘汰 lastAccessTime 最旧的那个
   - 讨论点:为什么不用完整 LRU(LinkedHashMap)?(答:采样近似的内存开销小很多)
4. **[单测]** SkipList 的插入/删除/范围查 每个都要有边界 case —— *AI 辅助但你要能自己想出来*

**W4 结束的离线自述题**
1. SkipList 的期望时间复杂度是 O(log n),它是怎么保证的?(答:每层向上晋升是 1/2 概率)
2. 你的 SkipList 里节点的最高层数怎么决定?
3. ZADD 一个已存在的 member 会发生什么?你的实现里这个逻辑在哪?
4. 采样 5 个 LRU 和"精确 LRU"在最坏情况下有多大误差?

**W4 关键陷阱**
- SkipList 是本项目**最容易翻车**的模块。**先看 antirez 的 t_zset.c 再动手**,不要看完 Java 版就抄。你要能讲清 forward 数组和 span 字段的区别(Redis 有 span,你可以选做)。

---

### W5(8/1 - 8/7):AOF + 简化 RDB

**目标**:kill 服务再启动,数据不丢

**任务清单**

1. **[AOF - 亲手]** 追加写机制
   - 每个写命令执行成功后,append 到 `appendonly.aof`
   - 三种 fsync 策略:always / everysec / no
   - **推荐 everysec**:主线程 append 到 buffer,后台线程每秒 fsync
2. **[AOF 重写 - 亲手]** 触发时机
   - `auto-aof-rewrite-percentage 100` + `auto-aof-rewrite-min-size 64mb`
   - 重写方式:不用 fork(Java 里 fork 复杂),用一个"后台线程扫内存生成新 AOF"
   - 讨论点:重写期间的写命令怎么办?(答:主线程写命令进 rewrite buffer,后台完成后主线程把 buffer append 到新文件)
3. **[RDB]** 简化版 snapshot
   - 单线程遍历 db,序列化到 `dump.rdb`(格式自己定,不用兼容官方 RDB)
   - 讨论点:如果这时候有写命令进来怎么办?(答:你实现里就阻塞;Redis 用 fork + COW)
4. **[启动]** 服务启动时先 load AOF,没有就 load RDB —— *AI 辅助*
5. **[测试]** 集成测试:写 100 条数据 → kill → 重启 → 验证数据在

**W5 结束的离线自述题**
1. AOF everysec 会丢多少数据?为什么?
2. 什么时候用 AOF 什么时候用 RDB?你的实现两个都开会有什么问题?
3. AOF 重写为什么会有"buffer"这个概念?去掉它会怎样?
4. Redis 用 fork 做 RDB,fork 之后父子进程共享内存吗?COW 是什么?

**W5 关键陷阱**
- AOF 的 fsync 策略搞错的话数据一致性讲不清。**一定要能画出"用户 write 到 pagecache 到磁盘"这三层**。
- 别真的实现 fork,Java 里不划算。老实说"我这里用后台线程 snapshot,和 Redis fork+COW 的对比是……"

---

### W6(8/8 - 8/14):JMH 压测 + 文档 + 简历包装

**目标**:一份压测报告 + 一份 README + 一段能在面试念出来的项目介绍

**任务清单**

1. **[JMH]** 压测脚本
   - SET 单线程 QPS
   - GET 单线程 QPS
   - MSET/MGET 批量吞吐
   - 对比目标:官方 Redis 6.2.6 同硬件下的数字
   - **必须写清硬件规格**(4C8G / M1 Pro / …)+ payload 大小(如 32 字节 key + 100 字节 value)
2. **[文档]** README.md 结构:
   - 项目定位(1 段)
   - 支持的命令列表(表格)
   - 架构图(Boss/Worker/Dispatcher/Storage/Persistence 分层)
   - 关键设计决策(为什么选 X 不选 Y,列 5-8 条)
   - 压测结果(表格 + 一段分析)
   - 已知限制 + 未来工作(诚实写:比如"未实现 Cluster"、"未实现事务")
3. **[GitHub]** 推到公开仓库,写 tag(如 v0.1)
4. **[简历]** 项目卡片:
   ```
   Mini-Redis(Java + Netty 手写单机 Redis)   github.com/xxx
   - 基于 Netty Reactor 模型实现 RESP2 协议解析、5 种数据结构 + 30 个命令、
     惰性 + 定期扫描双过期机制、采样近似 LRU 淘汰
   - 实现 AOF(everysec + 重写)+ 简化 RDB 持久化,重启零丢失
   - JMH 压测在 4C8G 环境达到官方 Redis 6.2.6 的 X% 吞吐(SET Yk QPS / GET Zk QPS)
   ```

**W6 结束的最终自述题**(20 分钟连讲不停)
1. 从 client 发一条 `SET foo bar` 到你返回 `+OK`,中间经过了哪些线程 / 类 / 方法?
2. 如果面试官让你现在从零重新设计这个系统,你会做哪些不同的决策?为什么?
3. 你的项目和真实 Redis 6.2.6 相比,最大的三个简化在哪里?每个简化的代价是什么?
4. 项目里你最不满意的一段代码是哪里?如果有一周时间你会怎么重构?

---

## 4. 拓展项(不阻塞 MVP,时间富余再做)

### 拓展 A:主从复制(W7-W10,4 周)
- 实现 PSYNC + disk-less 全量同步 + backlog 部分重同步
- 参考你在飞书里写的"disk-less: 内存 > 网络 > replica"
- **面试卡片强度翻倍**:直接可以对着实习经验讲

### 拓展 B:类 XPipe Keeper(W11-W14,4 周)
- 一个进程伪装成 Slave 向 Master 发 PSYNC 拉数据
- 缓存 replication stream 转发给其他 Slave
- **和你实习完全对齐**,面试官问 XPipe 你能直接讲这个

**建议**:W6 结束就开始投递,面试期间(9-10 月)边打面试边做拓展 A。转正结果出来后(10 月底)再看要不要做拓展 B。

---

## 5. 卡壳 SOP(遇到问题时按顺序做)

1. **描述问题**:在纸上写清"我在做什么 → 我期望什么 → 我看到什么 → 我已经尝试了什么"
2. **看参考实现**:去 https://github.com/wiqer/ef-redis 或 Redis 6.2.6 源码搜相关模块
3. **问 AI 但限定形式**:不要问"给我代码",而是问"我这里遇到 X,思路是什么?边界条件有哪些?"
4. **写小测试重现**:能重现的 bug 都不叫 bug,叫作业
5. **降级**:如果一个模块耗时超预期 50%,先跳过做下一个,标记 TODO,W6 回来补

---

## 6. 每周复盘 checklist(周日晚 30 分钟)

- [ ] 本周任务完成度:X/Y
- [ ] 本周实际写的行数 / AI 生成后自己 review 的行数
- [ ] 本周离线自述题答对几道?卡在哪?
- [ ] 本周最有价值的一个"卡壳→解决"故事(**面试可讲**,记下来)
- [ ] 下周风险预判:哪个任务最可能卡?怎么防?

---

## 7. 跨设备/跨模型协作流程

**核心原则**:PROGRESS.md 是唯一事实源,git 是同步通道。

**每次切换设备(居家↔公司)的动作**
1. 当前设备结束工作前:
   - `git checkout journal`
   - **通知 AI**"我今天做完 X,准备收工" → AI 按附录 C 规则更新 `PROGRESS/INDEX.md` 和 `PROGRESS/Wn.md`
   - `git add PROGRESS/ && git commit -m "docs: progress <一句话概括>" && git push`
   - `git checkout main`(如果之前在 main 分支)
2. 换到另一台设备:
   - `git checkout journal && git pull`
   - `git checkout main && git pull`

**每次开新 AI 对话的动作**
1. 贴 `PLAN.md` 全文(告诉 AI 项目总纲和维护规则)
2. 贴 `PROGRESS/INDEX.md` 全文(告诉 AI 你到哪了)
3. 说你的具体问题
4. AI 如果需要历史细节,会自己去读 `PROGRESS/Wn.md`

**AI 助手须知**:接手时请先读 `PROGRESS/INDEX.md` 的"当前状态"section,不要问用户"你做到哪一步了"。历史 log 按需读 `PROGRESS/Wn.md`,不要通读全部。

---

## 附录 A:给 AI 助手的角色 prompt(用户切模型时贴这段)

> 你是一位辅导中国大学生秋招的资深 Java 后端工程师 / 面试官。用户是川大软工大三,正在做一个 Mini-Redis 项目(Java + Netty)作为秋招简历主项目。用户的详细背景与项目 plan 见上文。
>
> 你的角色规则:
> 1. 用户在"必须亲手写的模块"(RESP 解析 / Netty 主循环 / 主从复制 / AOF 重写)向你要整段代码时,你必须**拒绝**,改为引导思路 + 让用户先写再 review。
> 2. 用户在"AI 辅助模块"(命令实现 / 单测 / 脚本 / 文档)向你要代码时,你可以给,但给完必须问一句"你能默写出核心逻辑吗?能的话就通过"。
> 3. 你答疑时应引用 Redis 6.2.6 官方实现的具体文件/函数,让用户能对照学习。
> 4. 用户是面试导向,你解释概念时应主动提"这个知识点面试常问,追问角度是……"。
> 5. 用户有 AI 依赖焦虑,你不需要不断提醒,但每周复盘时诚实评估他的独立性成长。
> 6. 用户完成 checklist 里一步时,主动列出下 2-4 个小点,方便他对照 + 提问。
> 7. **进度日志由你自主维护**,不要问用户"要不要记录"或"帮我记一下"。按附录 C 规则执行 —— 用户一句"我做完了 X"或"我收工了",你自动更新 `PROGRESS/INDEX.md` 和对应 `PROGRESS/Wn.md`,更新完提醒用户 `git commit + push`。

---

## 附录 B:面试卖点一句话(W6 定稿,提前占位)

**版本 A(保守)**
> Mini-Redis:Java + Netty 从零实现单机 Redis,含 RESP2 协议、5 种数据结构 + 30 个命令、AOF/RDB 持久化、近似 LRU 淘汰,JMH 压测达官方 X% 吞吐。

**版本 B(带实习联动)**
> Mini-Redis:Java + Netty 手写单机 Redis,深度对照携程 XPipe 中 Keeper 的伪 Slave 机制学习 Redis 复制协议;实现了 5 种数据结构 + 30 个命令 + AOF/RDB,JMH 压测在 4C8G 达 Redis 6.2.6 的 X% 吞吐。

---

## 附录 C:AI 自主维护 `PROGRESS/` 目录的规则

> 用户明确不手动维护此目录。**AI 助手必须严格遵守以下规则**,做到用户只需一句"我做完了 X"或"我今天收工"就能触发进度更新。

### C.1 目录结构

```
PROGRESS/
├── INDEX.md      # 总览:当前状态 + 阶段完成度 + 面试卡片累积。用户只看这个。
├── W0.md         # 环境搭建期
├── W1.md         # W1 每日日志
├── W2.md
├── ...
└── W6.md
```

### C.2 触发时机

AI 在以下场景**主动**更新 `PROGRESS/`,不需要用户显式要求:
- 用户说"我做完了 X / X 好了 / 完成了 X" → 更新对应 Wn.md 的日 log + 更新 INDEX.md 的"当前状态"
- 用户说"今天收工 / 明天再做 / 我要休息了" → 补齐当天日 log + 更新 INDEX.md 的"下一步"
- 用户说"我遇到 X 卡住了" + 后续解决 → 在日 log 里记录"卡壳→解决"故事;若有面试价值,同步抽到 INDEX.md 的"面试可讲的卡壳→解决故事"section
- 用户切换设备前(说"我要回家了 / 我到公司了") → 立即更新 INDEX 状态,便于下一台设备接手
- **每周日**(用户提及"复盘"或系统日期到 W 边界) → 在对应 Wn.md 末尾加"周复盘"section,勾选"每周复盘 checklist"6 项

### C.3 具体更新规则

**INDEX.md 只维护以下 3 个 section,其他不写**:
1. **当前状态**(3-5 行,每次都刷新):今日日期 / 阶段 / 进行中 / 卡壳 / 下一步
2. **阶段完成度**(勾选表,只在跨周时改)
3. **面试可讲的卡壳→解决故事**(累积,不删)

**Wn.md 结构固定**:
```
# W<n> <主题>(<起止日期>)

## 本周目标(从 PLAN.md 抄过来,不删)

## 每日 log

### YYYY-MM-DD(周X)@ <设备位置>
- 做完的事(bullet points)
- 卡壳→解决(如有)
- 未解决 / 遗留

### YYYY-MM-DD(周X)@ <设备位置>
- ...

## 周复盘(周日填,基于 PLAN.md §6 checklist)
- 任务完成度: X/Y
- 独立编码 vs AI 生成比例:X%
- 离线自述题答对几道
- 本周最有价值的"卡壳→解决"故事
- 下周风险预判
```

**日 log 长度**:每天 5-15 行 bullet points,不写作文。**如果用户当天没做实质工作(纯环境/开会/摸鱼),明确写"今日无进展",不要虚构充数**。

### C.4 归档 / 迁移规则

- 每周开工前(用户到新一周),AI 主动新建 `Wn.md`(用户不用管)
- INDEX.md 里"阶段完成度"跨周时手动打勾,不要漏
- **绝对不删除历史 Wn.md**,即使内容已经不重要(面试期回顾时可能用到)
- 如果 INDEX.md 里"当前状态"section 意外膨胀了(超过 10 行),AI 要主动精简回 3-5 行

### C.5 AI 自我核查(每次更新后必做)

- [ ] INDEX.md 的"当前状态"是不是最新今日?
- [ ] "下一步"是不是具体到可执行动作(而不是"继续 W1")?
- [ ] Wn.md 的日 log 是不是当天日期?没有虚构其他日期?
- [ ] 更新完是否提醒用户 `git commit + push`?

### C.6 用户的责任(极简)

- **一句话触发**:"我做完了 X" / "今天收工" / "我到家了" / "我卡在 X"
- **手动 git**:AI 更新文件后,用户负责在 `journal` 分支 commit + push
- **偶尔口味调整**:如果 AI 记录不合口味,用户可以说"这条别写"或"补一条",AI 立即改

---

**文档结束。用户与 AI 助手每次对话请先阅读本文档全部内容 + `PROGRESS/INDEX.md` 再回答。**
