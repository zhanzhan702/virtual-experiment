-- V9 低压场景补第 3、4 步：架设围栏并悬挂标识牌、三步式验电
--
-- 用途：已按旧版 04 脚本建过库的环境，执行本脚本补齐。
-- 幂等：目标 step_order 已存在时不会重复插入，可重复执行。
--
-- 分值 120 秒/20 分、90 秒/15 分为暂定；模板总分变为 90 分，
-- 成绩按「原始分 ÷ 步骤分合计 × 100」换算，百分比口径不受影响。
--
-- 注意（Windows）：mysql 客户端默认按系统编码解析脚本文件，中文会报
--   ERROR 1366 (HY000) Incorrect string value
-- 执行时请显式指定字符集：
--   mysql --default-character-set=utf8mb4 -u root virtual_experiment < 09_low_resident_box_steps.sql

-- 步骤 3：架设围栏并悬挂标识牌
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'), 3, 'ERECT_FENCE', '架设围栏并悬挂标识牌', 120, 20.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 3
);

-- 步骤 4：三步式验电
INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'), 4, 'THREE_STEP_CHECK', '三步式验电', 90, 15.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 4
);
