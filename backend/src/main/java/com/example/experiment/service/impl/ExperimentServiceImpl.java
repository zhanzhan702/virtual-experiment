package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

    // 更新（统计字段累加，支持存档恢复后继续计时）
    stepRecord.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
    stepRecord.setDurationSeconds(
        (stepRecord.getDurationSeconds() != null ? stepRecord.getDurationSeconds() : 0)
            + (dto.getDurationSeconds() != null ? dto.getDurationSeconds() : 0));
    stepRecord.setOperationCount(
        (stepRecord.getOperationCount() != null ? stepRecord.getOperationCount() : 0)
            + (dto.getOperationCount() != null ? dto.getOperationCount() : 0));
    stepRecord.setErrorCount(
        (stepRecord.getErrorCount() != null ? stepRecord.getErrorCount() : 0)
            + (dto.getErrorCount() != null ? dto.getErrorCount() : 0));
    stepRecord.setScore(dto.getScore() != null ? new java.math.BigDecimal(dto.getScore()) : null);
    stepRecord.setFinishedAt(LocalDateTime.now());
    userExperimentStepsMapper.updateById(stepRecord);

    // 实验级草稿/工作票数据（result_data 从步骤表移到实验表）：
    // 工作票步骤（order=1）提交数据写入 ticket_data 保留；任何步骤提交都清空当前草稿
    // （草稿必须显式 set null：MyBatis-Plus updateById 默认跳过 null 字段，直接赋值不会清空）
    var expRecord = userExperimentsMapper.selectById(dto.getExperimentId());
    if (expRecord != null) {
      var stepDef = stepsMapper.selectById(stepRecord.getStepId());
      boolean isTicketStep =
          stepDef != null && stepDef.getStepOrder() != null && stepDef.getStepOrder() == 1;
      if (isTicketStep && dto.getResultData() != null) {
        expRecord.setTicketData(dto.getResultData());
        userExperimentsMapper.updateById(expRecord);
      }
      userExperimentsMapper.update(
          null,
          new LambdaUpdateWrapper<UserExperiments>()
              .eq(UserExperiments::getId, dto.getExperimentId())
              .set(UserExperiments::getDraftData, null));
    }

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

    // 只更新数据字段（统计字段累加），不设置 finishedAt / score / status
    stepRecord.setDurationSeconds(
        (stepRecord.getDurationSeconds() != null ? stepRecord.getDurationSeconds() : 0)
            + (dto.getDurationSeconds() != null ? dto.getDurationSeconds() : 0));
    stepRecord.setOperationCount(
        (stepRecord.getOperationCount() != null ? stepRecord.getOperationCount() : 0)
            + (dto.getOperationCount() != null ? dto.getOperationCount() : 0));
    stepRecord.setErrorCount(
        (stepRecord.getErrorCount() != null ? stepRecord.getErrorCount() : 0)
            + (dto.getErrorCount() != null ? dto.getErrorCount() : 0));
    // 存档时当前步骤标记为未完成（防已提交步骤残留完成态导致恢复逻辑跳过）
    stepRecord.setStatus(0);
    userExperimentStepsMapper.updateById(stepRecord);

    // 草稿写到实验表（只存当前存档步骤内容，不占步骤表）
    var expRecord = userExperimentsMapper.selectById(dto.getExperimentId());
    if (expRecord != null) {
      expRecord.setDraftData(dto.getResultData());
      userExperimentsMapper.updateById(expRecord);
    }
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
          finishedSteps.stream().map(UserExperimentSteps::getStepId).collect(Collectors.toSet());
      var nextStep =
          allSteps.stream().filter(s -> !finishedIds.contains(s.getId())).findFirst().orElse(null);
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
    var exp = userExperimentsMapper.selectById(experimentId);
    if (exp == null) return;
    // 仅允许删除未完成的实验，已完成的受保护
    if (exp.getStatus() != null && exp.getStatus() == 1) {
      throw new RuntimeException("已完成实验不可删除");
    }
    userExperimentStepsMapper.delete(
        new LambdaQueryWrapper<UserExperimentSteps>()
            .eq(UserExperimentSteps::getExperimentId, experimentId));
    userExperimentsMapper.deleteById(experimentId);
  }

  @Override
  public Map<String, Object> getStepDraftData(String experimentId, String stepId) {
    // result_data 已移到实验表：草稿按实验 ID 查（stepId 参数保留兼容，不再按步骤区分）
    var exp = userExperimentsMapper.selectById(experimentId);
    if (exp == null || exp.getDraftData() == null) return Map.of();
    try {
      return new ObjectMapper().readValue(exp.getDraftData(), Map.class);
    } catch (Exception e) {
      return Map.of();
    }
  }

  @Override
  public Integer getTotalDuration(String experimentId) {
    var steps =
        userExperimentStepsMapper.selectList(
            new LambdaQueryWrapper<UserExperimentSteps>()
                .eq(UserExperimentSteps::getExperimentId, experimentId)
                .select(UserExperimentSteps::getDurationSeconds));
    return steps.stream()
        .map(s -> s.getDurationSeconds() != null ? s.getDurationSeconds() : 0)
        .reduce(0, Integer::sum);
  }

  @Override
  public List<Map<String, Object>> getExperimentSteps(String experimentId) {
    var exp = userExperimentsMapper.selectById(experimentId);
    if (exp == null) return Collections.emptyList();
    var steps =
        stepsMapper.selectList(
            new LambdaQueryWrapper<ExperimentSteps>()
                .eq(ExperimentSteps::getTemplateId, exp.getTemplateId())
                .orderByAsc(ExperimentSteps::getStepOrder));
    return steps.stream()
        .map(
            s ->
                Map.<String, Object>of(
                    "stepId", s.getId(),
                    "stepOrder", s.getStepOrder(),
                    "stepName", s.getStepName()))
        .collect(Collectors.toList());
  }

  @Override
  public void completeExperiment(String experimentId) {
    var exp = userExperimentsMapper.selectById(experimentId);
    if (exp == null) throw new RuntimeException("实验不存在");

    // 汇总各步骤总时长、总分（累加现有值，支持存档恢复后继续计时）
    var steps =
        userExperimentStepsMapper.selectList(
            new LambdaQueryWrapper<UserExperimentSteps>()
                .eq(UserExperimentSteps::getExperimentId, experimentId));
    int totalDuration = 0;
    java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
    for (var s : steps) {
      if (s.getDurationSeconds() != null) totalDuration += s.getDurationSeconds();
      if (s.getScore() != null) totalScore = totalScore.add(s.getScore());
    }

    exp.setStatus(1);
    exp.setEndTime(LocalDateTime.now());
    exp.setTotalDuration(totalDuration);
    exp.setScore(totalScore);
    userExperimentsMapper.updateById(exp);
  }
}
