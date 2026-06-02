package com.example.experiment.controller;

import com.example.experiment.dto.LoginDTO;
import com.example.experiment.dto.RegisterDTO;
import com.example.experiment.entity.UserRoles;
import com.example.experiment.entity.Users;
import com.example.experiment.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private UserService userService;
    //注册
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO dto){
        if(dto.getUsername()==null || dto.getUsername().isBlank()){
            return ResponseEntity.badRequest().body(Map.of("message","用户名不能为空"));
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "姓名不能为空"));
        }
        if (userService.existsByUsername(dto.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }

        Users user=new Users();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setGender(dto.getGender());
        user.setStudentNo(dto.getStudentNo());
        user.setOrgId(dto.getOrgId());

        Users saved = userService.register(user);
        return ResponseEntity.ok(Map.of(
                "message", "注册成功",
                "userId", saved.getId(),
                "username", saved.getUsername()
        ));
    }

    //登录检验和session存储
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto, HttpSession session){

        if(dto.getUsername()==null || dto.getUsername().isBlank()){
            return ResponseEntity.badRequest().body(Map.of("message","用户名不能为空"));
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码不能为空"));
        }
        Users user=userService.findByUsername(dto.getUsername());
        if(user==null){
            return ResponseEntity.status(404)
                    .body(Map.of("message", "用户名不存在"));
        }
        boolean passwordMatch = dto.getPassword().equals(user.getPassword());
        if(!passwordMatch){
            return ResponseEntity.status(401)
                    .body(Map.of("message","用户名或密码错误"));
        }

        session.setAttribute("userId",user.getId());
        session.setAttribute("userName",user.getName());
        session.setAttribute("userCodes",userService.getUserRoleCodes(user.getId()));

        return ResponseEntity.ok(Map.of(
                "message", "登录成功",
                "username", user.getUsername(),
                "userRoleCodes", userService.getUserRoleCodes(user.getId())
        ));
    }
}
