package com.example.experiment.exception;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 *
 * <p>响应体统一为 {@code {"message": "..."}}，与 AuthController 现有风格保持一致， 前端可直接取 {@code
 * err.response.data.message} 展示。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 业务异常：状态码由异常自身携带 */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<?> handleApiException(ApiException e) {
    return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
  }

  /**
   * 参数校验失败（@Valid）。
   *
   * <p>把字段错误拼成一句话，避免前端收到 Spring 默认的错误结构还要自己解析。 多条错误用「；」连接。
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getDefaultMessage() == null ? err.getField() : err.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("；"));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("message", message.isEmpty() ? "参数校验失败" : message));
  }
}
