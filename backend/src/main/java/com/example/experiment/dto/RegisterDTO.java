package com.example.experiment.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String gender;
    private String studentNo;
    private String orgId;
}
