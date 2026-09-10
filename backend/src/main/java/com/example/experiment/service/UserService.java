package com.example.experiment.service;

import com.example.experiment.dto.auth.LoginDTO;
import com.example.experiment.dto.auth.LoginVO;
import com.example.experiment.dto.auth.RegisterDTO;
import com.example.experiment.dto.auth.UserVO;
import com.example.experiment.entity.Users;
import java.util.List;

public interface UserService {

  boolean existsByUsername(String username);

  Users findByUsername(String username);

  List<String> getUserRoleCodes(String userId);

  /**
   * 用户的最高角色层级（多角色取最大值）。
   *
   * <p>无角色或角色 level 为 null 时返回 0（视为无权限）。
   */
  int getMaxLevel(String userId);

  /** 按 ID 查询用户 */
  Users findById(String userId);

  Users register(Users user);

  /** 注册（DTO版本）：自动分配学生角色 */
  Users register(RegisterDTO dto);

  /** 登录：校验密码，返回 token */
  LoginVO login(LoginDTO dto);

  /** Entity → VO（不含 maxLevel，调用方按需补充） */
  UserVO toUserVO(Users user);
}
