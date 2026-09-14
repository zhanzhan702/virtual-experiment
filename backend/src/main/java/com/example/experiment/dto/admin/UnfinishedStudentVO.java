package com.example.experiment.dto.admin;

import lombok.Data;

/** 班级概况里「未完成」名单中的一名学生。 */
@Data
public class UnfinishedStudentVO {

  private String userId;
  private String name;
  private String studentNo;

  /** 高压未完成 */
  private Boolean missingHigh;

  /** 低压未完成 */
  private Boolean missingLow;
}
