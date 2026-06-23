package com.example.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求。
 *
 * @author system
 */
@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest {

    @Schema(description = "refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
