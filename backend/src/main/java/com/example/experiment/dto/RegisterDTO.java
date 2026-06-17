package com.example.experiment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDTO {
  @NotBlank(message = "用户名不能为空")
  private String username;

  @NotBlank(message = "密码不能为空")
  @Size(min = 6, message = "密码至少6位")
  private String password;

  @NotBlank(message = "姓名不能为空")
  private String name;

  private String phone;
  private String email;
  private String gender;
  private String studentNo;
  private String orgId;
}
