package com.example.experiment.service;

import com.example.experiment.dto.auth.ChangePasswordDTO;
import com.example.experiment.dto.auth.LoginDTO;
import com.example.experiment.dto.auth.LoginVO;
import com.example.experiment.dto.auth.RegisterDTO;
import com.example.experiment.dto.auth.UpdateProfileDTO;
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

  /**
   * 自助改密：修改当前登录用户自己的密码。
   *
   * <p>必须验证原密码 —— 否则任何拿到 token 的人都能改掉密码，把真正的主人锁在系统外。 校验不通过时抛 {@link
   * com.example.experiment.exception.ApiException}（400）。
   */
  void changePassword(String userId, ChangePasswordDTO dto);

  /**
   * 自助改资料：修改当前登录用户自己的姓名、性别、生日、手机号、邮箱。
   *
   * <p>可改字段由 {@link UpdateProfileDTO} 的字段本身界定 —— 学号、班级、用户名不在其中，改不了。
   *
   * @return 更新后的用户信息，供前端直接刷新本地 store，省去再调一次 /me
   */
  UserVO updateProfile(String userId, UpdateProfileDTO dto);

  /** Entity → VO（不含 maxLevel，调用方按需补充） */
  UserVO toUserVO(Users user);
}
