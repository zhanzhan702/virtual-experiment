package com.example.experiment.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求当前用户的 maxLevel ≥ 指定角色的 level 才放行。
 *
 * <p>可标注在 Controller 类上（整个类生效），也可标注在单个方法上（方法级优先）。 由 {@link AuthInterceptor} 校验，不通过时返回 403
 * {"message":"权限不足"}。
 *
 * <p>用法：{@code @RequireRole(Role.TEACHER)} —— 教师及以上可访问。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
  Role value();
}
