package com.example.experiment.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.experiment.entity.ExperimentSteps;
import com.example.experiment.entity.ExperimentTemplates;
import com.example.experiment.mapper.ExperimentStepsMapper;
import com.example.experiment.mapper.ExperimentTemplatesMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 开发环境：自动插入测试实验模板和步骤（如果不存在） */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final ExperimentTemplatesMapper templatesMapper;
  private final ExperimentStepsMapper stepsMapper;

  public static final String TEST_TEMPLATE_CODE = "HV_TRAIN_V1";

  @Override
  public void run(String... args) {
    var existing =
        templatesMapper.selectOne(
            new LambdaQueryWrapper<ExperimentTemplates>()
                .eq(ExperimentTemplates::getCode, TEST_TEMPLATE_CODE));
    if (existing != null) {
      System.out.println("[DataInit] 模板 " + TEST_TEMPLATE_CODE + " 已存在，跳过");
      return;
    }

    // 插入模板
    var template = new ExperimentTemplates();
    template.setId(UUID.randomUUID().toString().replace("-", ""));
    template.setCode(TEST_TEMPLATE_CODE);
    template.setName("高压训练场景V1");
    template.setCategory("high_voltage");
    template.setMode("training");
    template.setVersion("1.0");
    template.setDescription("高压场景下的用电信息采集终端安装与调试训练模式");
    templatesMapper.insert(template);
    System.out.println("[DataInit] 模板已插入: " + TEST_TEMPLATE_CODE);

    // 只插入步骤1（后续步骤由 Flyway V3 或手动补充）
    insertStep(template.getId(), 1, "FILL_TICKET", "填写工作票", 600, 25);
    System.out.println("[DataInit] 步骤1已插入");
  }

  private void insertStep(
      String id, int order, String code, String name, int seconds, double score) {
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
