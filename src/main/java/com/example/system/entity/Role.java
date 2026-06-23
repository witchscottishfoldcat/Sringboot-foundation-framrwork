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
@TableName("role")
@Schema(description = "角色实体")
public class Role {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "角色ID", example = "1")
    private Long id;

    @TableField("role_name")
    @Schema(description = "角色名称", example = "ADMIN")
    private String roleName;

    @TableField("role_code")
    @Schema(description = "角色代码", example = "admin")
    private String roleCode;

    @TableField("description")
    @Schema(description = "角色描述", example = "系统管理员")
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