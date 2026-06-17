package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_roles")
public class UserRoles {
  private String userId;
  private String roleId;
}
