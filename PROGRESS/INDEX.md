# 进度总览

> **AI 助手接手时,只需要读本文件 + `PLAN.md`。**
> 需要历史细节时再按需读对应 `PROGRESS/Wn.md`。
> 维护规则见 `PLAN.md` 附录 C(用户不手动维护本目录,AI 自主维护)。

---

## 当前状态

- **今日**:2026-07-06(周一)@ 公司 Windows
- **阶段**:W1 骨架 + RESP 协议(7/3-7/10)
- **进行中**:RESP 编解码器已完成并经 AI code review 重构;主线代码已推 main(commits a0d4df3, cf381fc)
- **卡壳**:无
- **下一步**:CommandHandler + Pipeline 挂载 → redis-cli PING/ECHO 端到端验证。今晚是否推进未定,可能明天(2026-07-07)继续

---

## 阶段完成度

- [x] **W0 环境搭建**(2026-07-03 完成)—— 详见 [W0.md](W0.md)
- [ ] **W1 骨架 + RESP 协议**(7/3-7/10)—— 见 [W1.md](W1.md);已完成 4/5(骨架 + 解码 + 编码 + 单测),剩 CommandHandler 端到端
- [ ] **W2 String + Hash + 命令分发**(7/11-7/17)
- [ ] **W3 List + Set + 过期机制**(7/18-7/24)
- [ ] **W4 ZSet + SkipList + LRU 淘汰**(7/25-7/31)
- [ ] **W5 AOF + 简化 RDB**(8/1-8/7)
- [ ] **W6 JMH 压测 + 文档 + 简历**(8/8-8/14)

---

## 面试可讲的卡壳→解决故事(累积中)

> AI 归档周日志时,把有面试价值的一条抽到这里。W6 简历定稿时挑最好的 2-3 个用。

- **RESP 半包处理的契约演化**(W1)—— 最初 `decodeSimpleString` 只有 `$` 型返回半包信号,`+` 型半包时静默继续,导致 Array 内嵌套 SimpleString 半包会状态错乱。修复时统一契约:所有 helper 返回 boolean,顶层单次 mark/reset,任一 helper 说 false 就全局 reset。配套引入 `decodeOne` 递归分派,自然支持 nested Array。**面试角度**:"状态机契约设计"、"一次 mark/reset 覆盖多层结构"、"从边界 case 反推架构缺陷"。
- **Netty 异常传播的隐藏包装**(W1)—— decoder 抛的 `RespProtocolException` 被 Netty 包装成 `DecoderException`,测试用 `assertThatThrownBy().isInstanceOf(RespProtocolException.class)` 会失败,必须用 `hasRootCauseInstanceOf`。**面试角度**:"Netty pipeline 异常传播机制"、"AssertJ 三种异常断言的差异"、"库封装边界如何影响测试"。
- **Git pull 错误合并到 main 的救援**(W1 阶段)—— 居家在 main 分支上误 `git pull origin journal`,产生 merge commit 混入 doc 内容。因为没 push,用 `git reset --hard origin/main` 干净回滚。**面试角度**:"reset vs revert 的选择"、"公共分支只能 revert,私人未 push 用 reset"、"pull 默认 merge 的坑,推荐 `pull.rebase true`"。
