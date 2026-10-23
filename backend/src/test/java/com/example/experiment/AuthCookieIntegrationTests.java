package com.example.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.experiment.config.*;
import com.example.experiment.controller.AuthController;
import com.example.experiment.controller.ExperimentController;
import com.example.experiment.dto.auth.LoginVO;
import com.example.experiment.dto.auth.UserVO;
import com.example.experiment.entity.Users;
import com.example.experiment.exception.ApiException;
import com.example.experiment.exception.GlobalExceptionHandler;
import com.example.experiment.service.ExperimentService;
import com.example.experiment.service.UserService;
import com.example.experiment.utils.JwtUtils;
import com.example.experiment.utils.UserContext;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 使用真实过滤器、鉴权与控制器链路，业务服务替身避免依赖数据库。 */
class AuthCookieIntegrationTests {

  private static final String ORIGIN = "http://localhost:5173";
  private static final String LOGIN_BODY =
      "{\"username\":\"student1\",\"password\":\"student123\"}";
  private UserService users;
  private ExperimentService experiments;
  private MockMvc mvc;
  private AuthCookie authCookie;

  @BeforeEach
  void setUp() {
    UserContext.clear();
    users = mock(UserService.class);
    experiments = mock(ExperimentService.class);
    authCookie = new AuthCookie(false);
    var originFilter = new RequestOriginFilter(List.of(ORIGIN, "https://lab.example.com"));
    var interceptor = new AuthInterceptor(new SecurityWhitelist(), authCookie);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new AuthController(users, authCookie),
                new ExperimentController(experiments),
                new RoleProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .addInterceptors(interceptor)
            .addFilters(originFilter, new WebConfig(interceptor, originFilter).corsFilter())
            .build();
    UserVO user = new UserVO();
    user.setId("student-id");
    user.setUsername("student1");
    user.setMaxLevel(10);
    LoginVO login = new LoginVO();
    login.setUser(user);
    login.setRoles(List.of("student"));
    login.setRedirectUrl("/experiment");
    when(users.login(any())).thenReturn(login);
    when(users.findById("student-id")).thenReturn(new Users());
    when(users.toUserVO(any())).thenReturn(user);
    when(users.getMaxLevel("student-id")).thenReturn(10);
    when(users.getUserRoleCodes("student-id")).thenReturn(List.of("student"));
  }

  @Test
  void loginIssuesHttpOnlyCookieWithoutLeakingTokenAndRestoresUser() throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/auth/login")
                    .header("Origin", ORIGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(LOGIN_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.user.username").value("student1"))
            .andExpect(jsonPath("$.roles[0]").value("student"))
            .andExpect(jsonPath("$.redirectUrl").value("/experiment"))
            .andReturn();
    Cookie cookie = result.getResponse().getCookie(AuthCookie.NAME);
    assertThat(cookie).isNotNull();
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.getSecure()).isFalse();
    assertThat(cookie.getPath()).isEqualTo("/api");
    assertThat(cookie.getDomain()).isNull();
    assertThat(cookie.getMaxAge()).isEqualTo(86400);
    assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    assertThat(JwtUtils.getUserId(cookie.getValue())).isEqualTo("student-id");
    SecretKey key = (SecretKey) ReflectionTestUtils.getField(JwtUtils.class, "KEY");
    var claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(cookie.getValue()).getPayload();
    assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
        .isEqualTo(86400000);
    mvc.perform(get("/api/auth/me").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.id").value("student-id"));
    assertThat(UserContext.getUserId()).isNull();
  }

  @Test
  void failedLoginClearsCookie() throws Exception {
    when(users.login(any())).thenThrow(new RuntimeException("用户名或密码错误"));
    mvc.perform(
            post("/api/auth/login")
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0))
        .andExpect(jsonPath("$.token").doesNotExist());
  }

  @Test
  void invalidLoginInputDoesNotIssueCookie() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    verify(users, never()).login(any());
  }

  @Test
  void missingCookieAndBearerOnlyAreRejected() throws Exception {
    mvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token(10)))
        .andExpect(status().isUnauthorized());
    verify(users, never()).findById(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "malformed", "eyJhbGciOiJIUzI1NiJ9.invalid.signature"})
  void invalidCookieIsRejectedAndCleared(String token) throws Exception {
    mvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
  }

  @Test
  void tamperedAndExpiredSignedCookiesAreRejected() throws Exception {
    String token = token(10);
    String tampered = (token.startsWith("a") ? "b" : "a") + token.substring(1);
    SecretKey key = (SecretKey) ReflectionTestUtils.getField(JwtUtils.class, "KEY");
    String expired =
        Jwts.builder()
            .subject("student-id")
            .expiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(key)
            .compact();
    for (String invalid : List.of(tampered, expired)) {
      mvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookie.NAME, invalid)))
          .andExpect(status().isUnauthorized())
          .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
    }
  }

  @Test
  void deletedUserClearsCookie() throws Exception {
    when(users.findById("student-id")).thenReturn(null);
    mvc.perform(get("/api/auth/me").cookie(credential(10)))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
  }

  @Test
  void rolesRemainEnforcedAndContextIsClearedOnRejection() throws Exception {
    mvc.perform(get("/api/test/teacher").cookie(credential(10)))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    assertThat(UserContext.getUserId()).isNull();
    mvc.perform(get("/api/test/teacher").cookie(credential(20)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("student-id"));
    mvc.perform(get("/api/test/admin").cookie(credential(20))).andExpect(status().isForbidden());
    assertThat(UserContext.getUserId()).isNull();
    mvc.perform(get("/api/test/admin").cookie(credential(30))).andExpect(status().isOk());
    assertThat(UserContext.getUserId()).isNull();
    mvc.perform(get("/api/test/teacher")).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/logout",
        "/api/experiment/start"
      })
  void unsafeEndpointsRequireOriginEvenWhenPublic(String path) throws Exception {
    mvc.perform(
            post(path)
                .cookie(credential(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY))
        .andExpect(status().isForbidden());
    verifyNoInteractions(users, experiments);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "null",
        "",
        "https://evil.example",
        "http://localhost:5173.evil.example",
        "http://localhost:5174",
        "http://localhost:5173/",
        "http://user@localhost:5173",
        "%%%"
      })
  void untrustedOriginCannotFallBackToTrustedReferer(String origin) throws Exception {
    mvc.perform(
            post("/api/auth/logout").header("Origin", origin).header("Referer", ORIGIN + "/admin"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {"null", "https://evil.example/page", "http://localhost:5173.evil.example/page"})
  void untrustedRefererIsRejected(String referer) throws Exception {
    var request = post("/api/auth/logout");
    if (referer != null) request.header("Referer", referer);
    mvc.perform(request).andExpect(status().isForbidden());
  }

  @Test
  void trustedRefererAndConfiguredHttpsOriginAreAccepted() throws Exception {
    mvc.perform(post("/api/auth/logout").header("Referer", ORIGIN + "/admin/profile?q=1"))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
    mvc.perform(post("/api/auth/logout").header("Origin", "https://lab.example.com"))
        .andExpect(status().isOk());
  }

  @Test
  void corsOnlyAllowsTrustedPreflight() throws Exception {
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    mvc.perform(
            options("/api/auth/login")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  @Test
  void logoutIsIdempotentWithoutCookieOrWithExpiredCookie() throws Exception {
    for (int i = 0; i < 2; i++) {
      mvc.perform(post("/api/auth/logout").header("Origin", ORIGIN))
          .andExpect(status().isOk())
          .andExpect(cookie().maxAge(AuthCookie.NAME, 0))
          .andExpect(cookie().httpOnly(AuthCookie.NAME, true))
          .andExpect(cookie().path(AuthCookie.NAME, "/api"));
    }
    mvc.perform(
            post("/api/auth/logout")
                .header("Origin", ORIGIN)
                .cookie(new Cookie(AuthCookie.NAME, "expired")))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
  }

  @Test
  void successfulPasswordChangeClearsCookie() throws Exception {
    mvc.perform(
            put("/api/auth/password")
                .header("Origin", ORIGIN)
                .cookie(credential(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"oldPassword\":\"old123\",\"newPassword\":\"new123\",\"confirmPassword\":\"new123\"}"))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
    verify(users).changePassword(eq("student-id"), any());
  }

  @Test
  void invalidPasswordChangeDoesNotClearCookie() throws Exception {
    mvc.perform(
            put("/api/auth/password")
                .header("Origin", ORIGIN)
                .cookie(credential(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    verify(users, never()).changePassword(any(), any());
  }

  @Test
  void incorrectOldPasswordDoesNotClearCookie() throws Exception {
    doThrow(new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "原密码错误"))
        .when(users)
        .changePassword(any(), any());
    mvc.perform(
            put("/api/auth/password")
                .header("Origin", ORIGIN)
                .cookie(credential(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"oldPassword\":\"old123\",\"newPassword\":\"new123\",\"confirmPassword\":\"new123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
  }

  @Test
  void experimentEndpointsAcceptCookieWithoutBearer() throws Exception {
    mvc.perform(get("/api/experiment/unfinished").cookie(credential(10)))
        .andExpect(status().isOk());
    verify(experiments).getUnfinishedExperiments("student-id");
    mvc.perform(
            post("/api/experiment/start")
                .header("Origin", ORIGIN)
                .cookie(credential(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateCode\":\"HV_TRAIN_V1\"}"))
        .andExpect(status().isOk());
    verify(experiments).start(any(), eq("student-id"));
    for (String path : List.of("/step/submit", "/step/draft", "/complete")) {
      mvc.perform(
              post("/api/experiment" + path)
                  .header("Origin", ORIGIN)
                  .cookie(credential(10))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"experimentId\":\"experiment-id\"}"))
          .andExpect(status().isOk());
    }
    mvc.perform(
            get("/api/experiment/step/draft")
                .param("experimentId", "experiment-id")
                .param("stepId", "step-id")
                .cookie(credential(10)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/experiment/experiment-id/steps").cookie(credential(10)))
        .andExpect(status().isOk());
    when(experiments.getTotalDuration("experiment-id")).thenReturn(10);
    mvc.perform(get("/api/experiment/experiment-id/duration").cookie(credential(10)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.durationSeconds").value(10));
    mvc.perform(
            delete("/api/experiment/experiment-id").header("Origin", ORIGIN).cookie(credential(10)))
        .andExpect(status().isOk());
    assertThat(UserContext.getUserId()).isNull();
  }

  @Test
  void secureCookieUsesIdenticalDeletionAttributes() {
    AuthCookie secureCookie = new AuthCookie(true);
    assertThat(secureCookie.issue("jwt"))
        .contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/api", "Max-Age=86400")
        .doesNotContain("Domain=");
    assertThat(secureCookie.clear())
        .contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/api", "Max-Age=0")
        .doesNotContain("Domain=");
    assertThat(authCookie.read(new MockHttpServletRequest())).isNull();
  }

  private String token(int level) {
    return JwtUtils.generateToken(
        "student-id", List.of(level >= 20 ? "teacher" : "student"), level);
  }

  private Cookie credential(int level) {
    return new Cookie(AuthCookie.NAME, token(level));
  }

  @RestController
  @RequestMapping("/api/test")
  @RequireRole(Role.TEACHER)
  static class RoleProbeController {
    @GetMapping("/teacher")
    public Map<String, String> teacher() {
      return Map.of("userId", UserContext.getUserId());
    }

    @GetMapping("/admin")
    @RequireRole(Role.ADMIN)
    public Map<String, String> admin() {
      return Map.of("userId", UserContext.getUserId());
    }
  }
}
