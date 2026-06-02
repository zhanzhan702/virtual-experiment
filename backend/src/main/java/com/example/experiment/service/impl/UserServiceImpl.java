package com.example.experiment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.example.experiment.entity.Roles;
import com.example.experiment.entity.UserRoles;
import com.example.experiment.entity.Users;
import com.example.experiment.mapper.RolesMapper;
import com.example.experiment.mapper.UserRolesMapper;
import com.example.experiment.mapper.UsersMapper;
import com.example.experiment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRolesMapper userRolesMapper;
    private final RolesMapper rolesMapper;
    private final UsersMapper usersMapper;

    //查询用户是否存在
    @Override
    public boolean existsByUsername(String username) {
        return usersMapper.selectCount(
                new LambdaQueryWrapper<Users>()
                        .eq(Users::getUsername, username)
        ) > 0;
    }

    @Override
    public Users findByUsername(String username){
        return usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getUsername,username));
    }

    public List<String> getUserRoleCodes(String userId) {
        List<UserRoles> userRoles = userRolesMapper.selectList(new LambdaQueryWrapper<UserRoles>().eq(UserRoles::getUserId, userId));
        if (userRoles.isEmpty()) return Collections.emptyList();
        List<String> roleIds =userRoles.stream().map(UserRoles::getRoleId).collect(Collectors.toList());
        return rolesMapper.selectBatchIds(roleIds).stream().map(Roles::getCode).collect(Collectors.toList());
    }

    @Override
    public Users register(Users user){
        user.setId(UUID.randomUUID().toString().replace("-",""));
        usersMapper.insert(user);
        return user;
    }
}
