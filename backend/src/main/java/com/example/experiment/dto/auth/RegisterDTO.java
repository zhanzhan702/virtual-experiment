package com.example.experiment.dto.auth;

import com.example.experiment.utils.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
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

  /** 学号即登录账号，必填且全局唯一（唯一性在 AuthController 校验） */
  @NotBlank(message = "学号不能为空")
  private String studentNo;

  /** 性别：1 男 / 2 女，与 users.gender（TINYINT）取值一致 */
  @NotBlank(message = "请选择性别")
  private String gender;

  /** 组织：必须选到最低层班级（organization.type = class），层级校验在 UserServiceImpl */
  @NotBlank(message = "请选择所属班级")
  private String orgId;

  private String phone;
  private String email;

  @Past(message = "出生日期不能晚于今天")
  private LocalDate birthday;
}
