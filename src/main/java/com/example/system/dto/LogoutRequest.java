package com.example.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登出请求（携带两个令牌以便服务端读取 jti 写入黑名单）。
 *
 * @author system
 */
@Data
@Schema(description = "登出请求")
public class LogoutRequest {

    @Schema(description = "当前使用的 access token（可仅传 Authorization 头）")
    private String accessToken;

    @Schema(description = "refresh token")
    private String refreshToken;
}
