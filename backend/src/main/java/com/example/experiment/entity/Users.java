package com.example.experiment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class Users {
    private String id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String name;
    private String gender;
    private LocalDate birthday;
    private String studentNo;
    private String orgId;
    private LocalDateTime createdAt;
}
