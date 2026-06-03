package com.example.experiment.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private String id;
    private String username;
    private String name;
    private String phone;
    private String email;
    private String gender;
    private LocalDate birthday;
    private String studentNo;
    private String orgId;
    private String orgName;
    private LocalDateTime createdAt;
}
