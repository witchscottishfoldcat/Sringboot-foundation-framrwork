package com.example.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("\"user\"")
@Schema(description = "用户实体")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @TableField("username")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @TableField("password")
    @Schema(description = "密码", example = "123456")
    private String password;

    @TableField("email")
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @TableField("phone")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @TableField("nickname")
    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @TableField("avatar")
    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @TableField("role")
    @Schema(description = "用户角色", example = "ADMIN")
    private String role;

    @TableField(value = "must_change_password")
    @Schema(description = "是否需要首次登录后强制改密：0-否，1-是", example = "0")
    private Integer mustChangePassword;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人")
    private String createdBy;

    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人")
    private String updatedBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2023-01-01T12:00:00")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2023-01-01T12:00:00")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "是否删除：0-未删除，1-已删除", example = "0")
    private Integer deleted;
}