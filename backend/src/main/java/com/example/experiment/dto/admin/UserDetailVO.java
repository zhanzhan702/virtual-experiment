package com.example.experiment.dto.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 用户详情：基本信息 + 高/低压实验记录。 */
@Data
public class UserDetailVO {

  private String id;
  private String username;
  private String name;
  private String gender;
  private LocalDate birthday;
  private String studentNo;
  private String phone;
  private String email;

  /** 单位/班级完整路径 */
  private String orgName;

  private LocalDateTime createdAt;

  /** 高/低压实验记录，按开始时间倒序 */
  private List<UserExperimentVO> experiments;
}
