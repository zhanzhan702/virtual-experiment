package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.experiment.dto.admin.UpdatePasswordDTO;
import com.example.experiment.dto.admin.UserDetailVO;
import com.example.experiment.dto.admin.UserListVO;
import com.example.experiment.dto.admin.UserQueryDTO;
import com.example.experiment.entity.Users;
import com.example.experiment.exception.ApiException;
import com.example.experiment.mapper.UserExperimentsMapper;
import com.example.experiment.mapper.UsersMapper;
import com.example.experiment.service.AdminUserService;
import com.example.experiment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

  private final UsersMapper usersMapper;
  private final UserExperimentsMapper userExperimentsMapper;
  private final UserService userService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Override
  public IPage<UserListVO> listUsers(UserQueryDTO query, int myMaxLevel) {
    Page<UserListVO> page = new Page<>(query.safePage(), query.safeSize());
    return usersMapper.selectVisibleUsers(page, query, myMaxLevel);
  }

  @Override
  public UserDetailVO getUserDetail(String userId, int myMaxLevel) {
    UserDetailVO detail = requireVisibleUser(userId, myMaxLevel);
    detail.setExperiments(userExperimentsMapper.selectUserExperiments(userId));
    return detail;
  }

  @Override
  @Transactional
  public void resetPassword(String userId, UpdatePasswordDTO dto, int myMaxLevel) {
    requireVisibleUser(userId, myMaxLevel);

    // 前端已校验一遍，但服务端才是权威
    if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
      throw ApiException.badRequest("两次输入的密码不一致");
    }

    Users update = new Users();
    update.setId(userId);
    update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    usersMapper.updateById(update);
  }

  /**
   * 校验目标用户存在且在当前操作者可见范围内，返回其详情。
   *
   * <p>先判存在（404）再判层级（403）：调用方据此能区分「用户不存在」与「无权操作」，给出准确提示。
   * 反过来先判层级的话，无权用户会连「这个人是否存在」都无从得知，但那不是本接口需要防的威胁 —— 能调用本接口的人本就已通过登录与角色校验。
   */
  private UserDetailVO requireVisibleUser(String userId, int myMaxLevel) {
    UserDetailVO detail = usersMapper.selectUserDetail(userId);
    if (detail == null) {
      throw ApiException.notFound("用户不存在");
    }

    int targetLevel = userService.getMaxLevel(userId);
    if (targetLevel >= myMaxLevel) {
      throw ApiException.forbidden("无权操作该用户");
    }

    return detail;
  }
}
