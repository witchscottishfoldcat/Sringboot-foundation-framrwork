package com.example.system.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分页响应封装。
 *
 * @param <T> 记录类型
 * @author system
 */
@Data
@Builder
@Schema(description = "分页响应")
public class PageResult<T> {

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "当前页码（从 1 开始）")
    private long current;

    @Schema(description = "每页大小")
    private long size;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private long pages;

    public static <T> PageResult<T> from(Page<T> page) {
        return PageResult.<T>builder()
                .records(page.getRecords())
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .pages(page.getPages())
                .build();
    }
}
