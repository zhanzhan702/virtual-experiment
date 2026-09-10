package com.example.experiment.dto.auth;

import com.example.experiment.utils.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDTO {
  @NotBlank(message = "用户名不能为空")
  private String username;

  /** 强度规则见 {@link PasswordPolicy}，与重置密码表单共用同一套正则 */
  @NotBlank(message = "密码不能为空")
  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{6,20}$", message = "密码需6-20位，且同时包含字母和数字")
  private String password;

  @NotBlank(message = "姓名不能为空")
  private String name;

  private String phone;
  private String email;
  private String gender;
  private String studentNo;
  private String orgId;
}
