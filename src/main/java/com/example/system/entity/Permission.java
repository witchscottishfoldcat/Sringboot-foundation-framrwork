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
@TableName("permission")
@Schema(description = "权限实体")
public class Permission {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "权限ID", example = "1")
    private Long id;

    @TableField("permission_name")
    @Schema(description = "权限名称", example = "用户管理")
    private String permissionName;

    @TableField("permission_code")
    @Schema(description = "权限代码", example = "user:manage")
    private String permissionCode;

    @TableField("resource_type")
    @Schema(description = "资源类型", example = "menu")
    private String resourceType;

    @TableField("resource_url")
    @Schema(description = "资源URL", example = "/user/**")
    private String resourceUrl;

    @TableField("parent_id")
    @Schema(description = "父级ID", example = "0")
    private Long parentId;

    @TableField("description")
    @Schema(description = "权限描述", example = "用户管理权限")
    private String description;

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