package com.example.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 双令牌响应（access + refresh）。
 *
 * @author system
 */
@Data
@Builder
@Schema(description = "双令牌响应")
public class TokenPair {

    @Schema(description = "访问令牌（业务请求鉴权用，短期）")
    private String accessToken;

    @Schema(description = "刷新令牌（换取新令牌对用，长期）")
    private String refreshToken;

    @Schema(description = "access token 有效期（秒）")
    private Long expiresIn;

    @Schema(description = "refresh token 有效期（秒）")
    private Long refreshExpiresIn;

    @Schema(description = "令牌类型，固定 Bearer")
    @Builder.Default
    private String tokenType = "Bearer";
}
