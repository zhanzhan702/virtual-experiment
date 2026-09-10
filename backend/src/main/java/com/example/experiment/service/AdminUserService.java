package com.example.experiment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.experiment.dto.admin.UpdatePasswordDTO;
import com.example.experiment.dto.admin.UserDetailVO;
import com.example.experiment.dto.admin.UserListVO;
import com.example.experiment.dto.admin.UserQueryDTO;

/**
 * 管理端用户服务。
 *
 * <p>所有方法都接受 {@code myMaxLevel}（当前操作者的最高角色层级），由 Controller 从 UserContext 取出后传入。 越权判定统一为「目标用户的最高
 * level >= 我的 maxLevel」即拒绝。
 */
public interface AdminUserService {

  /** 分页查询可见用户 */
  IPage<UserListVO> listUsers(UserQueryDTO query, int myMaxLevel);

  /** 用户详情（含实验记录）；目标不可见或不存在时抛异常 */
  UserDetailVO getUserDetail(String userId, int myMaxLevel);

  /** 重置他人密码 */
  void resetPassword(String userId, UpdatePasswordDTO dto, int myMaxLevel);
}
