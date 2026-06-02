package com.example.experiment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.example.experiment.entity.Roles;
import com.example.experiment.entity.UserRoles;
import com.example.experiment.entity.Users;
import com.example.experiment.mapper.RolesMapper;
import com.example.experiment.mapper.UserRolesMapper;
import com.example.experiment.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public interface UserService {
    public boolean existsByUsername(String username) ;

    public Users findByUsername(String username);

    public List<String> getUserRoleCodes(String userId);

    public Users register(Users user);

}
