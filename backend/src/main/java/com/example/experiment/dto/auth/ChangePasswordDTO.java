package com.example.experiment.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 自助改密请求体（改自己的密码）。
 *
 * <p>与 {@link com.example.experiment.dto.admin.UpdatePasswordDTO} 刻意分开：后者是管理员重置**他人**密码，
 * 管理员本就不知道对方密码，不该有 {@code oldPassword} 字段。两者语义不同，共用一个 DTO 会互相牵制。
 *
 * <p>强度规则与注册、重置他人密码共用同一正则（见 {@code PasswordPolicy}）。
 */
@Data
public class ChangePasswordDTO {

  @NotBlank(message = "请输入原密码")
  private String oldPassword;

  @NotBlank(message = "密码不能为空")
  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{6,20}$", message = "密码需6-20位，且同时包含字母和数字")
  private String newPassword;

  @NotBlank(message = "请再次输入新密码")
  private String confirmPassword;
}
