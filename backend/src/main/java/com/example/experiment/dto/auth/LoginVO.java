package com.example.experiment.dto.auth;

import java.util.List;
import lombok.Data;

@Data
public class LoginVO {
  private String token;
  private UserVO user;
  private List<String> roles;
  private String redirectUrl;
}
