package com.example.experiment.service;

import com.example.experiment.dto.LoginDTO;
import com.example.experiment.dto.LoginVO;
import com.example.experiment.dto.RegisterDTO;
import com.example.experiment.entity.Users;

import java.util.List;

public interface UserService {

    boolean existsByUsername(String username);

    Users findByUsername(String username);

    List<String> getUserRoleCodes(String userId);

    Users register(Users user);

    /** 注册（DTO版本）：自动分配学生角色 */
    Users register(RegisterDTO dto);

    /** 登录：校验密码，返回 token */
    LoginVO login(LoginDTO dto);
}
