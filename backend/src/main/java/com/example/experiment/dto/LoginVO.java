package com.example.experiment.dto;

import lombok.Data;
import java.util.List;

@Data
public class LoginVO {
    private String token;
    private UserVO user;
    private List<String> roles;
    private String redirectUrl;
}
