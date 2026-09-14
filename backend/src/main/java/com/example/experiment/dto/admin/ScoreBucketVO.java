package com.example.experiment.dto.admin;

import lombok.Data;

/**
 * 分数段分布中的一格。
 *
 * <p>{@code count} 是<b>分数个数</b>而非人数 —— 一个高、低压都完成的学生会贡献 2 个分数， 与 {@link
 * ClassOverviewVO#getAvgScore()} 的口径一致（见 AdminGradeServiceImpl 的说明）。
 */
@Data
public class ScoreBucketVO {

  /** 形如「优 (90-100)」 */
  private String label;

  private Integer count;

  /** 占全部有效分数的百分比（整数）；无有效分数时为 null */
  private Integer ratio;
}
