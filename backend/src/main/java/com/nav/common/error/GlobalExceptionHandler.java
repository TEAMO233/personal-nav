package com.nav.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理:把各类异常统一转成 {code, message} 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常,用其自带的状态码与错误码返回。
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        // 1. 按异常携带的状态码与错误码返回
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage()));
    }

    /**
     * 请求体校验失败,返回 400 与首个字段错误信息。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        // 1. 取第一个字段错误信息
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst().map(FieldError::getDefaultMessage).orElse("参数校验失败");
        // 2. 返回 400
        return ResponseEntity.badRequest().body(new ApiError("VALIDATION_ERROR", message));
    }

    /**
     * 上传文件超过大小上限,返回 413。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        // 1. 返回 413 与统一错误码
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError("MEDIA_TOO_LARGE", "文件超过大小上限"));
    }

    /**
     * 登录认证失败,返回 401(不区分用户名或密码错误,避免信息泄露)。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        // 1. 统一提示
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("AUTH_FAILED", "用户名或密码错误"));
    }

    /**
     * 方法级权限不足,返回 403。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        // 1. 返回 403
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("FORBIDDEN", "无权限访问"));
    }

    /**
     * 兜底异常,记录日志并返回 500,不向前端泄露堆栈。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        // 1. 记录服务端日志
        log.error("未处理的异常", ex);
        // 2. 返回通用 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("INTERNAL_ERROR", "服务器内部错误"));
    }
}
