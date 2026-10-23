package com.example.experiment.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.experiment.config.AuthCookie;
import com.example.experiment.config.AuthInterceptor;
import com.example.experiment.config.RequestOriginFilter;
import com.example.experiment.config.SecurityWhitelist;
import com.example.experiment.config.WebConfig;
import com.example.experiment.controller.AuthController;
import com.example.experiment.controller.ExperimentController;
import com.example.experiment.dto.auth.LoginDTO;
import com.example.experiment.dto.auth.LoginVO;
import com.example.experiment.dto.auth.UserVO;
import com.example.experiment.entity.Users;
import com.example.experiment.exception.GlobalExceptionHandler;
import com.example.experiment.service.ExperimentService;
import com.example.experiment.service.UserService;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** 仅存在于测试类路径的浏览器联调服务：真实 HTTP/Cookie 链路，业务服务不连接数据库。 */
@TestConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration(
    excludeName = {
      "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
      "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
    })
@Import({
  AuthController.class,
  ExperimentController.class,
  AuthCookie.class,
  AuthInterceptor.class,
  RequestOriginFilter.class,
  SecurityWhitelist.class,
  WebConfig.class,
  GlobalExceptionHandler.class
})
public class CookieBrowserTestServer {

  public static void main(String[] args) {
    SpringApplication.run(CookieBrowserTestServer.class, args);
  }

  @Bean
  UserService userService() {
    UserService service = mock(UserService.class);
    UserVO student = new UserVO();
    student.setId("browser-student");
    student.setUsername("cookie_student");
    student.setName("Cookie 测试学生");
    student.setMaxLevel(10);
    UserVO teacher = new UserVO();
    teacher.setId("browser-teacher");
    teacher.setUsername("cookie_teacher");
    teacher.setName("Cookie 测试教师");
    teacher.setMaxLevel(20);
    when(service.login(any()))
        .thenAnswer(
            invocation -> {
              LoginDTO dto = invocation.getArgument(0);
              UserVO user =
                  "cookie_student".equals(dto.getUsername())
                      ? student
                      : "cookie_teacher".equals(dto.getUsername()) ? teacher : null;
              if (user == null || !"cookie123".equals(dto.getPassword())) {
                throw new RuntimeException("用户名或密码错误");
              }
              LoginVO vo = new LoginVO();
              vo.setUser(user);
              vo.setRoles(List.of(user.getMaxLevel() == 10 ? "student" : "teacher"));
              vo.setRedirectUrl(user.getMaxLevel() == 10 ? "/experiment" : "/admin/profile");
              return vo;
            });
    for (UserVO user : List.of(student, teacher)) {
      Users entity = new Users();
      entity.setId(user.getId());
      when(service.findById(user.getId())).thenReturn(entity);
      when(service.toUserVO(entity)).thenReturn(user);
      when(service.getMaxLevel(user.getId())).thenReturn(user.getMaxLevel());
      when(service.getUserRoleCodes(user.getId()))
          .thenReturn(List.of(user.getMaxLevel() == 10 ? "student" : "teacher"));
    }
    return service;
  }

  @Bean
  ExperimentService experimentService() {
    return mock(ExperimentService.class);
  }
}
