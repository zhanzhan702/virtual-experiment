package com.example.experiment.dto.admin;

import java.math.BigDecimal;
import lombok.Data;

/** 学生成绩行：高压/低压各一组状态与分数。 */
@Data
public class StudentGradeVO {

  private String userId;
  private String username;
  private String name;

  /** 高压是否已完成（存在 status=1 的记录） */
  private Boolean highDone;

  /** 高压显示分（百分制）；未完成或无法换算时为 null */
  private BigDecimal highScore;

  /** 高压分是否来自人工改分 */
  private Boolean highManual;

  private Boolean lowDone;
  private BigDecimal lowScore;
  private Boolean lowManual;
}
