package com.example.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.system.entity.TokenBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 令牌黑名单 Mapper。
 *
 * @author system
 */
@Mapper
public interface TokenBlacklistMapper extends BaseMapper<TokenBlacklist> {
}
