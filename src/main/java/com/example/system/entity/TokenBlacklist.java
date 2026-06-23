package com.example.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 令牌黑名单（吊销表）。
 * <p>
 * access / refresh token 在登出或刷新轮换时，把其 jti 写入此表；鉴权链路每次都查一次，
 * 命中则视为无效令牌。{@code expireAt} 用于定期清理已自然过期的记录。
 *
 * @author system
 */
@Data
@Accessors(chain = true)
@TableName("token_blacklist")
@Schema(description = "令牌黑名单")
public class TokenBlacklist {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("jti")
    @Schema(description = "令牌唯一标识（JWT ID）")
    private String jti;

    @TableField("user_id")
    @Schema(description = "所属用户ID")
    private Long userId;

    @TableField("username")
    @Schema(description = "所属用户名")
    private String username;

    @TableField("token_type")
    @Schema(description = "令牌类型：access / refresh")
    private String tokenType;

    @TableField("expire_at")
    @Schema(description = "令牌原始过期时间（UTC）")
    private LocalDateTime expireAt;

    @TableField("reason")
    @Schema(description = "吊销原因：logout / refresh / manual")
    private String reason;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
