-- 05. result_data 从步骤表移到实验表（2026-08-11 用户确认）
-- 注意：新结构已合入 01_init_tables.sql（新建库无需执行本脚本）；
--       本脚本仅用于「已执行 01-04 的存量库」迁移
--
-- 原逻辑：user_experiment_steps.result_data 每步都存 JSON，提交后仍是冗余内容
-- 新逻辑：草稿只在存档时存（实验级一份），提交时清空；工作票提交数据单独保留
--
-- 说明：
-- - draft_data：当前存档步骤草稿（saveDraft 写入，submitStep 清空），同一时刻只有一个
-- - ticket_data：工作票提交数据（ticketNo + member1，submitStep 写入，不清空）
-- - user_experiment_steps.result_data 列已删除（后端实体同步移除字段）

ALTER TABLE user_experiments
    ADD COLUMN draft_data JSON COMMENT '当前存档步骤草稿（saveDraft写入，submitStep清空）' AFTER score,
    ADD COLUMN ticket_data JSON COMMENT '工作票提交数据（ticketNo+member1）' AFTER draft_data;

-- 步骤表 result_data 列删除（后端已不再读写该列）
ALTER TABLE user_experiment_steps
    DROP COLUMN result_data;
