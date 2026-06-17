package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.experiment.dto.ExperimentStartDTO;
import com.example.experiment.dto.ExperimentStartVO;
import com.example.experiment.dto.ExperimentStepSubmitDTO;
import com.example.experiment.entity.*;
import com.example.experiment.mapper.*;
import com.example.experiment.service.ExperimentService;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExperimentServiceImpl implements ExperimentService {

  private final ExperimentTemplatesMapper templatesMapper;
  private final ExperimentStepsMapper stepsMapper;
  private final UserExperimentsMapper userExperimentsMapper;
  private final UserExperimentStepsMapper userExperimentStepsMapper;

  @Override
  public ExperimentStartVO start(ExperimentStartDTO dto, String userId) {
    // 1. 查询模板（支持按 ID 或 code）
    ExperimentTemplates template;
    if (dto.getTemplateCode() != null) {
      template =
          templatesMapper.selectOne(
              new LambdaQueryWrapper<ExperimentTemplates>()
                  .eq(ExperimentTemplates::getCode, dto.getTemplateCode()));
    } else {
      template = templatesMapper.selectById(dto.getTemplateId());
    }
    if (template == null) throw new RuntimeException("实验模板不存在");

    // 2. 查询步骤
    var steps =
        stepsMapper.selectList(
            new LambdaQueryWrapper<ExperimentSteps>()
                .eq(ExperimentSteps::getTemplateId, dto.getTemplateId())
                .orderByAsc(ExperimentSteps::getStepOrder));

    // 3. 创建实验记录
    var record = new UserExperiments();
    record.setId(UUID.randomUUID().toString().replace("-", ""));
    record.setUserId(userId);
    record.setTemplateId(dto.getTemplateId());
    record.setStartTime(LocalDateTime.now());
    record.setStatus(0);
    userExperimentsMapper.insert(record);

    // 4. 预置所有步骤记录
    for (var step : steps) {
      var sr = new UserExperimentSteps();
      sr.setId(UUID.randomUUID().toString().replace("-", ""));
      sr.setExperimentId(record.getId());
      sr.setStepId(step.getId());
      sr.setStatus(0);
      userExperimentStepsMapper.insert(sr);
    }

    // 5. 返回
    var vo = new ExperimentStartVO();
    vo.setExperimentId(record.getId());
    vo.setTemplateName(template.getName());
    vo.setStartTime(record.getStartTime());
    vo.setSteps(
        steps.stream()
            .map(
                s -> {
                  var sb = new ExperimentStartVO.StepBrief();
                  sb.setStepId(s.getId());
                  sb.setStepName(s.getStepName());
                  sb.setStepOrder(s.getStepOrder());
                  return sb;
                })
            .collect(Collectors.toList()));
    return vo;
  }

  @Override
  public void submitStep(ExperimentStepSubmitDTO dto) {
    // 查找步骤记录
    var stepRecord =
        userExperimentStepsMapper.selectOne(
            new LambdaQueryWrapper<UserExperimentSteps>()
                .eq(UserExperimentSteps::getExperimentId, dto.getExperimentId())
                .eq(UserExperimentSteps::getStepId, dto.getStepId()));

    if (stepRecord == null) throw new RuntimeException("步骤记录不存在");

    // 首次提交时设置开始时间
    if (stepRecord.getStartedAt() == null) {
      if (dto.getStartedAt() != null) {
        String t = dto.getStartedAt().replace("Z", "").replace("T", " ").substring(0, 19);
        stepRecord.setStartedAt(
            LocalDateTime.parse(
                t, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      } else {
        stepRecord.setStartedAt(LocalDateTime.now());
      }
    }

    // 更新
    stepRecord.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    stepRecord.setDurationSeconds(dto.getDurationSeconds());
    stepRecord.setOperationCount(dto.getOperationCount());
    stepRecord.setErrorCount(dto.getErrorCount());
    stepRecord.setScore(dto.getScore() != null ? new java.math.BigDecimal(dto.getScore()) : null);
    stepRecord.setResultData(dto.getResultData());
    stepRecord.setFinishedAt(LocalDateTime.now());
    userExperimentStepsMapper.updateById(stepRecord);

    // 检查是否全部完成 → 更新 experiment 状态
    var unfinished =
        userExperimentStepsMapper.selectCount(
            new LambdaQueryWrapper<UserExperimentSteps>()
                .eq(UserExperimentSteps::getExperimentId, dto.getExperimentId())
                .eq(UserExperimentSteps::getStatus, 0));
    if (unfinished == 0) {
      var exp = userExperimentsMapper.selectById(dto.getExperimentId());
      if (exp != null) {
        exp.setStatus(1);
        exp.setEndTime(LocalDateTime.now());
        userExperimentsMapper.updateById(exp);
      }
    }
  }
}
