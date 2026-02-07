package com.shijie.transit.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获并处理 Controller 层抛出的各类异常，
 * 将其转换为标准的 {@link ApiResponse} 格式返回给前端。
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(TransitException.class)
  public ResponseEntity<ApiResponse<Void>> handleTransitException(TransitException ex, HttpServletRequest request) {
    ErrorCode errorCode = ex.getErrorCode();
    ApiResponse<Void> body = ApiResponse.error(errorCode, ex.getMessage(), clock.millis(), null);
    return ResponseEntity.status(errorCode.httpStatus()).body(body);
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
  public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex) {
    String message = null;
    if (ex instanceof MethodArgumentNotValidException manv) {
      message = firstFieldErrorMessage(manv.getBindingResult().getFieldError());
    } else if (ex instanceof BindException be) {
      message = firstFieldErrorMessage(be.getBindingResult().getFieldError());
    }
    ApiResponse<Void> body = ApiResponse.error(ErrorCode.VALIDATION_ERROR, message, clock.millis(), null);
    return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus()).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    ApiResponse<Void> body = ApiResponse.error(ErrorCode.BAD_REQUEST, ex.getMessage(), clock.millis(), null);
    return ResponseEntity.status(ErrorCode.BAD_REQUEST.httpStatus()).body(body);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
    ApiResponse<Void> body = ApiResponse.error(ErrorCode.UNAUTHORIZED, ex.getMessage(), clock.millis(), null);
    return ResponseEntity.status(ErrorCode.UNAUTHORIZED.httpStatus()).body(body);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    ApiResponse<Void> body = ApiResponse.error(ErrorCode.FORBIDDEN, ex.getMessage(), clock.millis(), null);
    return ResponseEntity.status(ErrorCode.FORBIDDEN.httpStatus()).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleOther(Exception ex) {
    ApiResponse<Void> body =
        ApiResponse.error(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), clock.millis(), null);
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.httpStatus()).body(body);
  }

  private String firstFieldErrorMessage(FieldError fieldError) {
    return Optional.ofNullable(fieldError).map(FieldError::getDefaultMessage).orElse(null);
  }
}
