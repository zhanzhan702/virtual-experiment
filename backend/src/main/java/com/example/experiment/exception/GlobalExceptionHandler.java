package com.example.experiment.exception;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
   * 请求体解析失败（JSON 格式错误、编码不是 UTF-8、类型不匹配等）。
   *
   * <p>不处理的话 Spring 会返回默认错误体 {@code {timestamp,status,error,path}}， 前端拿不到 {@code message}
   * 字段，只能显示「请求失败」这类无信息量的提示。
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> handleUnreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "请求内容格式不正确"));
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
