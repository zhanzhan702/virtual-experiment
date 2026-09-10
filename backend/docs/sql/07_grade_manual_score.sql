-- 07. 查看学生成绩：人工改分字段（2026-09-10）
--
-- 注意：本脚本所有变更均已同步至 01_init_tables.sql，重建库无需执行本脚本；
--       本脚本仅为「不重建库」的存量环境提供增量迁移，后期会删除。
--       数据库可整体重建时，直接执行 01 → 02 → 03 → 04 即可。
--
-- 量纲说明：
--   score        系统算出的原始加权分 = Σ 步骤模板score × 得分率，
--                量纲等于该模板步骤分合计（高压 24 步合计 228）
--   manual_score 教师人工改分，**百分制**（0-100）
--
-- 两者量纲不同是刻意的：模板步骤分值后期还会调整，若人工分存原始分，
-- 一旦模板分值变动，历史改分的含义就会漂移（老师当初给的 85 分会变成别的值）。
-- 存百分制则始终稳定，且与界面展示、教师输入的单位一致。
--
-- 展示优先级：manual_score ?? (score / 模板步骤分合计 × 100)
-- 撤销人工改分：将 manual_score / scored_by / scored_at 置 NULL 即可，原始 score 不受影响

ALTER TABLE user_experiments
    ADD COLUMN manual_score DECIMAL(5,2) NULL COMMENT '人工改分（百分制 0-100）' AFTER score,
    ADD COLUMN scored_by    BINARY(16)   NULL COMMENT '改分人' AFTER manual_score,
    ADD COLUMN scored_at    DATETIME     NULL COMMENT '改分时间' AFTER scored_by;

-- 补充原有 score 列的注释（存量库该列此前无注释）
ALTER TABLE user_experiments
    MODIFY COLUMN score DECIMAL(5,2) NULL COMMENT '系统原始加权分（量纲=模板步骤分合计）';
