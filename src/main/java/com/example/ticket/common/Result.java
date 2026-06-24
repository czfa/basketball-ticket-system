package com.example.ticket.common;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 * 用于统一API接口的响应格式，便于前端统一处理
 * 
 * @param <T> 响应数据的类型
 * @author chenyuhan
 * @since 2025-11-26
 */
public class Result<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 响应状态码
     * 200表示成功，其他值表示失败
     */
    private Integer code;
    
    /**
     * 响应消息
     * 用于描述操作结果或错误信息
     */
    private String message;
    
    /**
     * 响应数据
     * 成功时返回的业务数据
     */
    private T data;
    
    /**
     * 时间戳
     * 响应生成的时间戳（毫秒）
     */
    private Long timestamp;
    
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应（无数据）
     * 
     * @return Result对象
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }
    
    /**
     * 成功响应（带数据）
     * 
     * @param data 响应数据
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }
    
    /**
     * 成功响应（自定义消息）
     * 
     * @param message 自定义消息
     * @param data 响应数据
     * @return Result对象
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }
    
    /**
     * 失败响应（默认消息）
     * 
     * @return Result对象
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }
    
    /**
     * 失败响应（自定义消息）
     * 
     * @param message 错误消息
     * @return Result对象
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }
    
    /**
     * 失败响应（自定义状态码和消息）
     * 
     * @param code 状态码
     * @param message 错误消息
     * @return Result对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
    
    /**
     * 失败响应（使用ResultCode枚举）
     * 
     * @param resultCode 结果状态码枚举
     * @return Result对象
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }
    
    // Getter and Setter methods
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
