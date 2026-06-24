package com.example.ticket.exception;

import com.example.ticket.common.Result;
import com.example.ticket.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 用于统一处理系统中抛出的异常，并返回统一的响应格式
 * 
 * @author chenyuhan
 * @since 2025-11-26
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理运行时异常（业务异常）
     * 通常用于处理业务逻辑中的异常情况
     * 
     * @param e 运行时异常
     * @return 统一响应结果
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Object>> handleRuntimeException(RuntimeException e) {
        logger.error("业务异常：{}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.error(e.getMessage()));
    }
    
    /**
     * 处理参数异常
     * 通常用于处理参数校验失败的情况
     * 
     * @param e 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("参数错误：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage()));
    }
    
    /**
     * 处理所有未捕获的异常
     * 用于捕获系统中未知的异常，避免直接暴露给用户
     * 
     * @param e 异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception e) {
        logger.error("系统异常：{}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.error(ResultCode.ERROR));
    }
}
