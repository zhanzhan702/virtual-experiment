-- V10 低压场景补第 5 步：居民表箱内部操作
--
-- 用途：三步验电完成后进入表箱内部（Leafer UI 画布），先建立步骤记录，
-- 画布背景与具体操作内容待素材提供后补充。
-- 幂等：目标 step_order 已存在时不会重复插入，可重复执行。
--
-- step_code / step_name / 分值为占位值，内容确定后按需调整。
--
-- 注意（Windows）：mysql 客户端默认按系统编码解析脚本文件，中文会报
--   ERROR 1366 (HY000) Incorrect string value
-- 执行时请显式指定字符集：
--   mysql --default-character-set=utf8mb4 -u root virtual_experiment < 10_low_resident_box_inner_step.sql

INSERT INTO experiment_steps (id, template_id, step_order, step_code, step_name, required_seconds, score)
SELECT UUID_TO_BIN(UUID()), (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1'), 5, 'BOX_INNER_OPERATION', '居民表箱内部操作', 240, 20.00
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1
  FROM experiment_steps
  WHERE template_id = (SELECT id FROM experiment_templates WHERE code = 'LV_TRAIN_V1')
    AND step_order = 5
);
