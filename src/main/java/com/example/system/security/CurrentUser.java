package com.example.system.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于在 Controller 方法参数中直接注入当前登录用户主体。
 * <pre>
 * &#64;GetMapping("/me")
 * public Result&lt;?&gt; me(&#64;CurrentUser UserPrincipal principal) { ... }
 * </pre>
 *
 * @author system
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
    /**
     * 当未登录（匿名访问）时是否允许注入 null。默认 false（未登录直接抛 401）。
     */
    boolean required() default false;
}
