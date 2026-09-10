package com.example.experiment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 带 HTTP 状态码的业务异常。
 *
 * <p>用于区分「无权操作」(403) 与「用户不存在」(404) —— 两者若都返回 404，前端无法给出准确提示。 由 Controller 捕获后转成 {@code {"message":
 * "..."}}，与 AuthController 现有的响应结构保持一致。
 */
@Getter
public class ApiException extends RuntimeException {

  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static ApiException forbidden(String message) {
    return new ApiException(HttpStatus.FORBIDDEN, message);
  }

  public static ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, message);
  }

  public static ApiException badRequest(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, message);
  }
}
