package com.example.experiment.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 角色层级枚举：数值越大权限越高。
 *
 * <p>用户的可见范围统一判定为 {@code level < 当前用户的 maxLevel}， 因此管理员看不到其他管理员、老师只看到学生。
 *
 * <p>层级值必须与数据库 {@code roles.level} 保持一致（见 docs/sql/01_init_tables.sql 与 02_create_data.sql）。
 * 用枚举而非魔法数字，避免调用处写错数值。
 */
@Getter
@RequiredArgsConstructor
public enum Role {
  STUDENT(10),
  TEACHER(20),
  ADMIN(30),
  SUPER_ADMIN(40);

  private final int level;

  /** 由数据库中的角色 code 解析，未知 code 返回 null（不抛异常，避免脏数据导致 500） */
  public static Role fromCode(String code) {
    if (code == null) {
      return null;
    }
    for (Role role : values()) {
      if (role.name().equalsIgnoreCase(code)) {
        return role;
      }
    }
    return null;
  }
}
