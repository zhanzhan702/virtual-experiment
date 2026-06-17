package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("users")
public class Users {
  private String id;
  private String username;
  private String password;
  private String phone;
  private String email;
  private String name;
  private String gender;

  @TableField("birth_date")
  private LocalDate birthday;

  @TableField("student_no")
  private String studentNo;

  @TableField("org_id")
  private String orgId;

  @TableField("created_at")
  private LocalDateTime createdAt;
}
