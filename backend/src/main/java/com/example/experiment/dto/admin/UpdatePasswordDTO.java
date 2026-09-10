package com.example.experiment.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 管理员重置他人密码请求体。 */
@Data
public class UpdatePasswordDTO {

  /** 强度规则与注册表单一致（见 PasswordPolicy） */
  @NotBlank(message = "密码不能为空")
  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{6,20}$", message = "密码需6-20位，且同时包含字母和数字")
  private String newPassword;

  @NotBlank(message = "请再次输入密码")
  private String confirmPassword;
}
