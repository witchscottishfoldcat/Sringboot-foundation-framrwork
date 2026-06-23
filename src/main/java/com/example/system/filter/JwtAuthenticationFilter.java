package com.example.system.filter;

import com.example.system.security.CustomUserDetailsService;
import com.example.system.security.UserPrincipal;
import com.example.system.service.TokenBlacklistService;
import com.example.system.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器。
 * <p>
 * 解析请求头中的 access token：
 * <ol>
 *   <li>校验签名与过期（无效则不写入上下文，交由后续 EntryPoint 返回 401）。</li>
 *   <li>校验类型必须为 access。</li>
 *   <li>查询黑名单，已吊销则不写入上下文。</li>
 *   <li>从数据库加载最新权限（实时校验，避免权限变更滞后）。</li>
 *   <li>写入 SecurityContext。</li>
 * </ol>
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.parseClaims(token);
            // 必须是 access token；refresh token 不得用于业务请求
            if (!JwtUtil.TYPE_ACCESS.equals(claims.get("type", String.class))) {
                log.debug("非 access 类型令牌被拒绝：type={}", claims.get("type"));
                chain.doFilter(request, response);
                return;
            }
            String jti = claims.getId();
            if (tokenBlacklistService.isRevoked(jti)) {
                log.debug("令牌已被吊销：jti={}", jti);
                chain.doFilter(request, response);
                return;
            }
            String username = claims.getSubject();
            UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException ex) {
            log.debug("access token 已过期");
            // 不写入上下文；@PreAuthorize 受保护资源会触发 401
        } catch (JwtException ex) {
            log.debug("access token 无效：{}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("认证过程中发生异常：{}", ex.getMessage());
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
