package com.example.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应。
 * <p>
 * 不再返回 permissions 字段（避免泄露权限清单），前端通过 /auth/permissions 单独拉取。
 *
 * @author system
 */
@Data
@Builder
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "access token 有效期（秒）")
    private Long expiresIn;

    @Schema(description = "令牌类型，固定 Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "用户角色")
    private String role;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "是否需要首次登录后强制改密")
    @Builder.Default
    private Boolean mustChangePassword = false;
}
