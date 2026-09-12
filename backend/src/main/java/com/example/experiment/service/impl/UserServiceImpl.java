package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.experiment.dto.auth.ChangePasswordDTO;
import com.example.experiment.dto.auth.LoginDTO;
import com.example.experiment.dto.auth.LoginVO;
import com.example.experiment.dto.auth.RegisterDTO;
import com.example.experiment.dto.auth.UserVO;
import com.example.experiment.entity.Roles;
import com.example.experiment.entity.UserRoles;
import com.example.experiment.entity.Users;
import com.example.experiment.exception.ApiException;
import com.example.experiment.mapper.RolesMapper;
import com.example.experiment.mapper.UserRolesMapper;
import com.example.experiment.mapper.UsersMapper;
import com.example.experiment.service.UserService;
import com.example.experiment.utils.JwtUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRolesMapper userRolesMapper;
  private final RolesMapper rolesMapper;
  private final UsersMapper usersMapper;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Override
  public boolean existsByUsername(String username) {
    return usersMapper.selectCount(new LambdaQueryWrapper<Users>().eq(Users::getUsername, username))
        > 0;
  }

  @Override
  public Users findByUsername(String username) {
    return usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getUsername, username));
  }

  @Override
  public List<String> getUserRoleCodes(String userId) {
    List<UserRoles> userRoles =
        userRolesMapper.selectList(
            new LambdaQueryWrapper<UserRoles>().eq(UserRoles::getUserId, userId));
    if (userRoles.isEmpty()) return Collections.emptyList();
    List<String> roleIds =
        userRoles.stream().map(UserRoles::getRoleId).collect(Collectors.toList());
    return rolesMapper.selectBatchIds(roleIds).stream()
        .map(Roles::getCode)
        .collect(Collectors.toList());
  }

  @Override
  public int getMaxLevel(String userId) {
    if (userId == null) {
      return 0;
    }
    List<UserRoles> userRoles =
        userRolesMapper.selectList(
            new LambdaQueryWrapper<UserRoles>().eq(UserRoles::getUserId, userId));
    if (userRoles.isEmpty()) {
      return 0;
    }
    List<String> roleIds =
        userRoles.stream().map(UserRoles::getRoleId).collect(Collectors.toList());
    return rolesMapper.selectBatchIds(roleIds).stream()
        .map(Roles::getLevel)
        .filter(Objects::nonNull)
        .mapToInt(Integer::intValue)
        .max()
        .orElse(0);
  }

  @Override
  public Users findById(String userId) {
    if (userId == null) {
      return null;
    }
    return usersMapper.selectById(userId);
  }

  @Override
  public Users register(Users user) {
    user.setId(UUID.randomUUID().toString().replace("-", ""));
    // 密码 BCrypt 加密
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    usersMapper.insert(user);
    return user;
  }

  @Override
  @Transactional
  public void changePassword(String userId, ChangePasswordDTO dto) {
    // 先做不涉及查库的格式校验，再比对密码 —— BCrypt 是刻意设计成慢的，
    // 让格式错误的请求先被挡掉，避免无谓的计算开销
    if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
      throw ApiException.badRequest("两次输入的密码不一致");
    }

    Users user = usersMapper.selectById(userId);
    if (user == null) {
      throw ApiException.notFound("用户不存在");
    }

    if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
      throw ApiException.badRequest("原密码不正确");
    }

    if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
      throw ApiException.badRequest("新密码不能与原密码相同");
    }

    Users update = new Users();
    update.setId(userId);
    update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    usersMapper.updateById(update);
  }

  @Override
  public Users register(RegisterDTO dto) {
    // DTO → Entity
    Users user = new Users();
    user.setUsername(dto.getUsername());
    user.setPassword(dto.getPassword());
    user.setName(dto.getName());
    user.setPhone(dto.getPhone());
    user.setEmail(dto.getEmail());
    user.setStudentNo(dto.getStudentNo());
    user.setOrgId(dto.getOrgId());

    // 注册（含密码加密）
    Users saved = register(user);

    // 自动分配学生角色
    Roles studentRole =
        rolesMapper.selectOne(new LambdaQueryWrapper<Roles>().eq(Roles::getCode, "student"));
    if (studentRole != null) {
      UserRoles userRole = new UserRoles();
      userRole.setUserId(saved.getId());
      userRole.setRoleId(studentRole.getId());
      userRolesMapper.insert(userRole);
    }

    return saved;
  }

  @Override
  public LoginVO login(LoginDTO dto) {
    // 查询用户
    Users user = findByUsername(dto.getUsername());
    if (user == null) {
      throw new RuntimeException("用户名不存在");
    }

    // 校验密码（BCrypt）
    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      throw new RuntimeException("用户名或密码错误");
    }

    // 查询角色与最高层级
    List<String> roles = getUserRoleCodes(user.getId());
    int maxLevel = getMaxLevel(user.getId());

    // 生成 token（maxLevel 一并写入 claim，后续请求无需查库）
    String token = JwtUtils.generateToken(user.getId(), roles, maxLevel);

    // 组装 VO
    UserVO userVO = toUserVO(user);
    userVO.setMaxLevel(maxLevel);
    LoginVO loginVO = new LoginVO();
    loginVO.setToken(token);
    loginVO.setUser(userVO);
    loginVO.setRoles(roles);
    loginVO.setRedirectUrl(roles.contains("student") ? "/experiment" : "/admin");
    return loginVO;
  }

  @Override
  public UserVO toUserVO(Users user) {
    if (user == null) return null;
    UserVO vo = new UserVO();
    vo.setId(user.getId());
    vo.setUsername(user.getUsername());
    vo.setName(user.getName());
    vo.setPhone(user.getPhone());
    vo.setEmail(user.getEmail());
    vo.setGender(user.getGender());
    vo.setBirthday(user.getBirthday());
    vo.setStudentNo(user.getStudentNo());
    vo.setOrgId(user.getOrgId());
    vo.setCreatedAt(user.getCreatedAt());
    return vo;
  }
}
