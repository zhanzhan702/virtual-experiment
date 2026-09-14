package com.example.experiment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.experiment.dto.admin.ClassOverviewVO;
import com.example.experiment.dto.admin.ClassSummaryVO;
import com.example.experiment.dto.admin.StudentExperimentVO;
import com.example.experiment.dto.admin.StudentGradeVO;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端成绩服务。
 *
 * <p>界面上的分数一律为**百分制**（0-100），由系统原始加权分除以模板步骤分合计换算而来。 步骤分合计从 experiment_steps 实时求和，模板分值调整后历史分数自动跟随。
 */
public interface AdminGradeService {

  /**
   * 某节点下所有班级的成绩汇总。
   *
   * <p>传年级返回其下所有班级，传班级返回它自己，传学院返回其下所有年级的所有班级 —— 逻辑统一，不必按节点类型分支。 空班级也会返回（人数 0），否则新建的班级在页面上不可见。
   */
  List<ClassSummaryVO> getClassSummaries(String orgId, int myMaxLevel);

  /**
   * 单个班级的成绩概况：完成率、平均/最高/最低分、分数段分布、未完成名单。
   *
   * <p>{@code orgId} 必须是班级节点（传年级/学院会 404）—— 这个视图是为「点开一个班看它怎么样」设计的， 多班对比由 {@link #getClassSummaries}
   * 承担。
   *
   * <p>分数口径与 {@link #getClassSummaries} 一致，两处的平均分必然相同。
   */
  ClassOverviewVO getClassOverview(String orgId, int myMaxLevel);

  /** 某节点下学生的成绩分页（含无任何实验记录的学生） */
  IPage<StudentGradeVO> getStudentGrades(
      String orgId, String name, int page, int size, int myMaxLevel);

  /** 某学生的完成历史（含进行中的记录） */
  List<StudentExperimentVO> getStudentExperiments(String userId, int myMaxLevel);

  /**
   * 人工改分。{@code manualScore} 为 null 表示撤销改分。
   *
   * <p>校验：实验记录存在（404）、记录所属学生可见（403）、分数在 0-100 内（400）。
   */
  void updateScore(String experimentId, BigDecimal manualScore, String operatorId, int myMaxLevel);
}
