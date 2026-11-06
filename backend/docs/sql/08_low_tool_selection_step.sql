-- V8 低压场景补第 2 步：工器具选择
--
-- 用途：已按旧版 04 脚本建过库的环境，执行本脚本补齐第 2 步。
-- 幂等：已存在 step_order = 2 时不会重复插入，可重复执行。
--
-- 分值 180 秒 / 30 分为暂定，与高压第 2 步同值；模板总分变为 55 分，
-- 成绩按「原始分 ÷ 步骤分合计 × 100」换算，百分比口径不受影响。
--
-- 注意（Windows）：mysql 客户端默认按系统编码解析脚本文件，中文会报
--   ERROR 1366 (HY000) Incorrect string value
-- 执行时请显式指定字符集：
--   mysql --default-character-set=utf8mb4 -u root virtual_experiment < 08_low_tool_selection_step.sql

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()),
       0x00000000000000000000000000000032,
       2,
       'SELECT_TOOLS',
       '工器具选择',
       180,
       30.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM experiment_steps
  WHERE template_id = 0x00000000000000000000000000000032
    AND step_order = 2
);
