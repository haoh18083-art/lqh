该目录用于存放 Flyway SQL 迁移脚本。

建议迁移顺序：

1. 先按现有 MySQL 表结构建立基线脚本。
2. 再补充 `system_settings`、`medicine_orders`、`ai_chat_sessions` 等增量脚本。
3. 所有 Mongo 集合结构变更单独在迁移文档里维护，不混入 Flyway。

当前阶段仅搭骨架，尚未生成正式迁移 SQL。
