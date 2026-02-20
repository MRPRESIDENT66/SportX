package com.example.sportx.Config;

import com.example.sportx.Entity.vo.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "请求参数不合法"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldError() == null
                ? "请求参数不合法"
                : exception.getBindingResult().getFieldError().getDefaultMessage();
        return Result.error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return Result.error(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return Result.error(exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return Result.error("数据重复，请勿重复提交");
        }
        String lower = message.toLowerCase();
        if (lower.contains("uk_challenge_participation_user_challenge")) {
            return Result.error("你已报名该挑战，请勿重复操作");
        }
        if (lower.contains("uk_spot_favorites_user_spot")) {
            return Result.error("你已收藏该场馆");
        }
        return Result.error("数据重复，请勿重复提交");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.error("系统异常，请稍后重试");
    }
}
