package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.experiment.dto.experiment.ExperimentStartDTO;
import com.example.experiment.dto.experiment.ExperimentStartVO;
import com.example.experiment.dto.experiment.ExperimentStepSubmitDTO;
import com.example.experiment.dto.experiment.UnfinishedExperimentVO;
import com.example.experiment.entity.*;
import com.example.experiment.mapper.*;
import com.example.experiment.service.ExperimentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;
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
                .eq(ExperimentSteps::getTemplateId, template.getId())
                .orderByAsc(ExperimentSteps::getStepOrder));

    // 3. 创建实验记录
    var record = new UserExperiments();
    record.setId(UUID.randomUUID().toString().replace("-", ""));
    record.setUserId(userId);
    record.setTemplateId(template.getId());
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

  @Override
  public void saveDraft(ExperimentStepSubmitDTO dto) {
    var stepRecord =
        userExperimentStepsMapper.selectOne(
            new LambdaQueryWrapper<UserExperimentSteps>()
                .eq(UserExperimentSteps::getExperimentId, dto.getExperimentId())
                .eq(UserExperimentSteps::getStepId, dto.getStepId()));
    if (stepRecord == null) throw new RuntimeException("步骤记录不存在");

    // 首次保存时设置开始时间
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

    // 只更新数据字段，不设置 finishedAt / score / status（保持草稿状态）
    stepRecord.setDurationSeconds(dto.getDurationSeconds());
    stepRecord.setOperationCount(dto.getOperationCount());
    stepRecord.setErrorCount(dto.getErrorCount());
    stepRecord.setResultData(dto.getResultData());
    userExperimentStepsMapper.updateById(stepRecord);
  }

  @Override
  public List<UnfinishedExperimentVO> getUnfinishedExperiments(String userId) {
    var experiments =
        userExperimentsMapper.selectList(
            new LambdaQueryWrapper<UserExperiments>()
                .eq(UserExperiments::getUserId, userId)
                .eq(UserExperiments::getStatus, 0)
                .orderByDesc(UserExperiments::getStartTime));

    List<UnfinishedExperimentVO> result = new ArrayList<>();
    for (var exp : experiments) {
      var template = templatesMapper.selectById(exp.getTemplateId());
      var allSteps =
          stepsMapper.selectList(
              new LambdaQueryWrapper<ExperimentSteps>()
                  .eq(ExperimentSteps::getTemplateId, exp.getTemplateId())
                  .orderByAsc(ExperimentSteps::getStepOrder));
      var finishedSteps =
          userExperimentStepsMapper.selectList(
              new LambdaQueryWrapper<UserExperimentSteps>()
                  .eq(UserExperimentSteps::getExperimentId, exp.getId())
                  .eq(UserExperimentSteps::getStatus, 1));

      var vo = new UnfinishedExperimentVO();
      vo.setExperimentId(exp.getId());
      vo.setTemplateName(template != null ? template.getName() : "");
      vo.setCategory(template != null ? template.getCategory() : "");
      vo.setStartTime(exp.getStartTime());
      vo.setCompletedSteps(finishedSteps.size());
      vo.setTotalSteps(allSteps.size());

      // 找第一个未完成的步骤
      var finishedIds =
          finishedSteps.stream()
              .map(UserExperimentSteps::getStepId)
              .collect(Collectors.toSet());
      var nextStep =
          allSteps.stream()
              .filter(s -> !finishedIds.contains(s.getId()))
              .findFirst()
              .orElse(null);
      if (nextStep != null) {
        vo.setNextStepId(nextStep.getId());
        vo.setNextStepOrder(nextStep.getStepOrder());
        vo.setNextStepName(nextStep.getStepName());
      }
      result.add(vo);
    }
    return result;
  }

  @Override
  public void deleteExperiment(String experimentId) {
    userExperimentStepsMapper.delete(
        new LambdaQueryWrapper<UserExperimentSteps>()
            .eq(UserExperimentSteps::getExperimentId, experimentId));
    userExperimentsMapper.deleteById(experimentId);
  }

  @Override
  public Map<String, Object> getStepDraftData(String experimentId, String stepId) {
    var step =
        userExperimentStepsMapper.selectOne(
            new LambdaQueryWrapper<UserExperimentSteps>()
                .eq(UserExperimentSteps::getExperimentId, experimentId)
                .eq(UserExperimentSteps::getStepId, stepId));
    if (step == null || step.getResultData() == null) return Map.of();
    try {
      return new ObjectMapper().readValue(step.getResultData(), Map.class);
    } catch (Exception e) {
      return Map.of();
    }
  }
}
