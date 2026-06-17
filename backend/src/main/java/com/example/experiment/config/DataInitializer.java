package com.example.experiment.config;

import com.example.experiment.entity.ExperimentSteps;
import com.example.experiment.entity.ExperimentTemplates;
import com.example.experiment.mapper.ExperimentStepsMapper;
import com.example.experiment.mapper.ExperimentTemplatesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 开发环境：自动插入测试实验模板和步骤（如果不存在）
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ExperimentTemplatesMapper templatesMapper;
    private final ExperimentStepsMapper stepsMapper;

    public static final String TEST_TEMPLATE_ID = "a1b2c3d4e5f67890abcdef1234567890";

    @Override
    public void run(String... args) {
        var existing = templatesMapper.selectById(TEST_TEMPLATE_ID);
        if (existing != null) {
            System.out.println("[DataInit] 测试模板已存在，跳过");
            return;
        }

        // 插入模板
        var template = new ExperimentTemplates();
        template.setId(TEST_TEMPLATE_ID);
        template.setCode("high_voltage_1");
        template.setName("高压场景第一版");
        template.setCategory("high_voltage");
        template.setMode("training");
        template.setVersion("1.0");
        template.setDescription("高压场景下的用电信息采集终端安装与调试");
        templatesMapper.insert(template);
        System.out.println("[DataInit] 模板已插入: " + TEST_TEMPLATE_ID);

        // 插入步骤
        insertStep("b1b2c3d4e5f67890abcdef1234567891", 1, "step_01", "填写工作票", 600, 25);
        insertStep("b1b2c3d4e5f67890abcdef1234567892", 2, "step_02", "工作负责人布置工作任务", 300, 10);
        insertStep("b1b2c3d4e5f67890abcdef1234567893", 3, "step_03", "选择工器具", 600, 20);
        insertStep("b1b2c3d4e5f67890abcdef1234567894", 4, "step_04", "计量柜操作", 900, 25);
        insertStep("b1b2c3d4e5f67890abcdef1234567895", 5, "step_05", "计量小室接线操作", 1200, 20);
        System.out.println("[DataInit] 步骤已插入: 5 条");
    }

    private void insertStep(String id, int order, String code, String name, int seconds, double score) {
        var step = new ExperimentSteps();
        step.setId(id);
        step.setTemplateId(TEST_TEMPLATE_ID);
        step.setStepOrder(order);
        step.setStepCode(code);
        step.setStepName(name);
        step.setRequiredSeconds(seconds);
        step.setScore(new java.math.BigDecimal(score));
        stepsMapper.insert(step);
    }
}
