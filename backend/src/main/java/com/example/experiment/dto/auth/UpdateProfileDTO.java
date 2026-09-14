package com.example.experiment.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

/**
 * 自助改资料请求体（改自己的资料）。
 *
 * <p><b>字段即白名单</b>：DTO 里没有用户名、学号、班级（orgId）、注册时间 —— 这些由教务或管理员确定，
 * 本人不可改。请求体里即便多传这些字段也会被丢弃，不存在「绕过前端改学号」的路径。
 *
 * <p>手机号与邮箱<b>允许清空</b>：正则写成 {@code ^$|...} 而非直接 {@code ^1[3-9]\d{9}$}。
 * 后者对空字符串不匹配，用户想删掉手机号时会被自己的校验挡住。 {@code @Email} 对 null 与空串都判为通过，无需额外处理。
 */
@Data
public class UpdateProfileDTO {

  @NotBlank(message = "请输入姓名")
  @Size(max = 50, message = "姓名最多 50 个字符")
  private String name;

  /** 与 users.gender（TINYINT）取值一致：0 未设置 / 1 男 / 2 女 */
  @Pattern(regexp = "^$|^[012]$", message = "性别取值不合法")
  private String gender;

  /** 留空表示不设置生日 */
  private LocalDate birthday;

  @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
  private String phone;

  @Email(message = "邮箱格式不正确")
  @Size(max = 100, message = "邮箱最多 100 个字符")
  private String email;
}
