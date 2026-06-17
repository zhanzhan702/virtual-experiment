package com.example.experiment.dto;

import java.util.List;
import lombok.Data;

@Data
public class LoginVO {
  private String token;
  private UserVO user;
  private List<String> roles;
  private String redirectUrl;
}
