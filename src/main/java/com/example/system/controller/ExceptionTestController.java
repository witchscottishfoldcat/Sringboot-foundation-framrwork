package com.example.system.controller;

import com.example.system.common.ErrorCode;
import com.example.system.common.Result;
import com.example.system.exception.AuthException;
import com.example.system.exception.BusinessException;
import com.example.system.exception.SystemException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 异常测试控制器
 * 用于测试各种异常处理情况
 */
@RestController
@RequestMapping("/exception-test")
@Tag(name = "异常测试接口", description = "用于测试异常处理机制的接口")
public class ExceptionTestController {

    @Operation(summary = "测试业务异常", description = "抛出一个业务异常")
    @GetMapping("/business")
    public Result<String> testBusinessException(@RequestParam(defaultValue = "false") boolean withData) {
        if (withData) {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", 12345);
            data.put("operation", "test");
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "测试业务异常（带数据）", data);
        } else {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "测试业务异常（无数据）");
        }
    }

    @Operation(summary = "测试认证异常", description = "抛出一个认证异常")
    @GetMapping("/auth")
    public Result<String> testAuthException() {
        throw new AuthException(ErrorCode.UNAUTHORIZED, "测试认证异常");
    }

    @Operation(summary = "测试系统异常", description = "抛出一个系统异常")
    @GetMapping("/system")
    public Result<String> testSystemException() {
        throw new SystemException(ErrorCode.INTERNAL_SERVER_ERROR, "测试系统异常");
    }

    @Operation(summary = "测试空指针异常", description = "抛出一个空指针异常")
    @GetMapping("/nullpointer")
    public Result<String> testNullPointerException() {
        String str = null;
        // 这会抛出NullPointerException
        int length = str.length();
        return Result.success("Length: " + length);
    }

    @Operation(summary = "测试数组越界异常", description = "抛出一个数组越界异常")
    @GetMapping("/array")
    public Result<String> testArrayIndexOutOfBoundsException() {
        int[] array = new int[5];
        // 这会抛出ArrayIndexOutOfBoundsException
        return Result.success(String.valueOf(array[10]));
    }

    @Operation(summary = "测试数字格式异常", description = "抛出一个数字格式异常")
    @GetMapping("/numberformat")
    public Result<String> testNumberFormatException(@RequestParam String number) {
        // 这会抛出NumberFormatException
        int num = Integer.parseInt(number);
        return Result.success("解析成功: " + num);
    }

    @Operation(summary = "测试算术异常", description = "抛出一个算术异常")
    @GetMapping("/arithmetic")
    public Result<String> testArithmeticException() {
        // 这会抛出ArithmeticException
        int result = 10 / 0;
        return Result.success(String.valueOf(result));
    }

    @Operation(summary = "测试类型转换异常", description = "抛出一个类型转换异常")
    @GetMapping("/classcast")
    public Result<String> testClassCastException() {
        Object obj = "Hello";
        // 这会抛出ClassCastException
        Integer num = (Integer) obj;
        return Result.success(String.valueOf(num));
    }

    @Operation(summary = "测试IllegalArgumentException", description = "抛出一个IllegalArgumentException")
    @GetMapping("/illegal-argument")
    public Result<String> testIllegalArgumentException(@RequestParam String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("参数值不能为空");
        }
        return Result.success("参数值: " + value);
    }
}