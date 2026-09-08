package com.example.cloudhospital.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> biz(BizException e) { return ResponseEntity.badRequest().body(ApiResponse.fail(e.getCode(), e.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
        FieldError error = e.getBindingResult().getFieldError();
        if (error == null) return ResponseEntity.badRequest().body(ApiResponse.fail(40000, "参数校验失败"));
        String field = error.getField();
        String message = field.equals("items") ? "请至少添加一种药品" : error.getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.fail(40000, message));
    }
    @ExceptionHandler({ConstraintViolationException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponse<Void>> conflict(Exception e) { return ResponseEntity.badRequest().body(ApiResponse.fail(40000, "数据不合法或已存在")); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unknown(Exception e) { return ResponseEntity.internalServerError().body(ApiResponse.fail(50000, "系统异常，请稍后重试")); }
}
