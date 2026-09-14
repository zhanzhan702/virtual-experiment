package com.example.experiment.dto.admin;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 单个班级的成绩概况。选中组织树里的班级节点时，展示在成绩表上方。
 *
 * <p><b>分数口径与 {@link ClassSummaryVO#getAvgScore()} 严格一致</b>：把全班所有有效的 高压分与低压分放进同一批里统计（一个高、低压都完成的学生贡献
 * 2 个分数）。 若此处换成「先算每人平均、 再对人数求平均」，同一个班在汇总表和概况卡里会显示出两个不同的平均分。
 */
@Data
public class ClassOverviewVO {

  private Integer studentCount;

  private Integer highDone;
  private Integer lowDone;

  /** 完成率（整数百分比）；班级人数为 0 时为 null，而不是 0 */
  private Integer highDoneRate;

  private Integer lowDoneRate;

  /** 高、低压**均**完成的人数占比（整数百分比）——「全班都做完了」的比例 */
  private Integer overallDoneRate;

  /** 平均分 / 最高分 / 最低分（百分制）；无有效分数时为 null */
  private BigDecimal avgScore;

  private BigDecimal maxScore;
  private BigDecimal minScore;

  /** 分数段分布，固定 5 档（优/良/中/及格/不及格），空档 count 为 0 而非缺项 */
  private List<ScoreBucketVO> buckets;

  /** 高压或低压未完成的学生 */
  private List<UnfinishedStudentVO> unfinished;
}
