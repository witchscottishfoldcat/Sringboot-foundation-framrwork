# Java 安全管理系统

基于 Spring Boot + Spring Security + JWT + MyBatis Plus 的安全管理框架，支持 RBAC 权限控制、双令牌认证、双时区策略和多数据库。

## 核心功能

- 用户认证与授权（Spring Security + JWT 双令牌）
- RBAC 权限控制系统（角色-权限-用户三维管理）
- **双令牌认证**：access token（短期）+ refresh token（长期，支持轮换）
- **令牌吊销**：登出 / 刷新时把 jti 写入黑名单，鉴权链路实时校验
- **启动期密钥强校验**：生产环境弱/默认 JWT 密钥拒绝启动
- **双时区策略**：内部统一 UTC，对外可配置（默认 Asia/Shanghai）
- 多数据库支持（MySQL 和 PostgreSQL）
- 完善的异常处理机制（含 JWT / 认证异常统一 JSON 响应、敏感字段脱敏）
- 自动数据库表初始化
- RESTful API 设计
- Swagger API 文档

## 技术栈

- Spring Boot 3.3.5
- Spring Security（Method Security + JWT Filter）
- jjwt 0.12.6（HS512，双令牌 + jti）
- MyBatis Plus 3.5.7
- MySQL / PostgreSQL（JDBC 统一 serverTimezone=UTC）
- Lombok 1.18.38
- Hutool
- SpringDoc OpenAPI (Swagger) 2.6.0
- SLF4J + Logback (日志框架)

## 系统架构

### 权限模型

本系统采用标准的 RBAC（Role-Based Access Control）权限模型：
- **用户(User)**: 系统的使用者
- **角色(Role)**: 权限的集合
- **权限(Permission)**: 对资源的操作权限
- **用户角色关系(UserRole)**: 用户和角色的多对多关系
- **角色权限关系(RolePermission)**: 角色和权限的多对多关系

### 数据库支持

系统原生支持两种主流数据库：
- MySQL
- PostgreSQL

通过 Spring Profiles 实现无缝切换，适配不同数据库的语法差异。

## 快速开始

### 1. 环境准备

- JDK 17+
- Maven 3.6+
- MySQL 5.7+ 或 PostgreSQL 10+
- IDE (IntelliJ IDEA 推荐)

### 2. 配置 JWT 密钥（生产环境必填）

通过环境变量注入强密钥（HS512 要求 **≥ 64 字节**）：

```bash
export JWT_SECRET="<不少于64字节的随机字符串>"
```

> 生产环境（非 `dev` profile）若密钥缺失 / 不足 64 字节 / 命中弱密钥黑名单
> （`mySecretKey`、`secret`、`changeme` 等），应用**拒绝启动**。

### 3. 数据库配置

在 `src/main/resources/application.yml` 中配置数据库连接：

```yaml
spring:
  profiles:
    active: mysql  # 或 postgresql
```

修改对应数据库配置文件：
- MySQL: `application-mysql.yml`
- PostgreSQL: `application-postgresql.yml`

> JDBC 连接统一使用 `serverTimezone=UTC`，与内部时区策略保持一致。

### 4. 时区配置（可选）

```yaml
app:
  timezone:
    internal: UTC              # 内部固定 UTC，不建议改动
    external: Asia/Shanghai    # 对外（JSON 序列化）时区，默认上海，可配置
    external-serialization-enabled: true
```

### 5. 启动应用

```bash
mvn spring-boot:run
```

系统会自动初始化数据库表结构和基础数据。

## API 接口

### 认证接口

- `POST /auth/login` - 用户登录（返回 access + refresh 双令牌）
- `POST /auth/register` - 用户注册
- `POST /auth/refresh` - 刷新令牌（refresh token 轮换，旧 token 立即吊销）
- `POST /auth/logout` - 登出（吊销当前 access / refresh token）
- `GET /auth/permissions` - 获取当前登录用户权限列表
- `GET /auth/info` - 获取用户信息

### 权限管理接口

- `GET /permission/list` - 获取权限列表
- `POST /permission/create` - 创建权限
- `PUT /permission/update/{id}` - 更新权限
- `DELETE /permission/delete/{id}` - 删除权限

### 角色管理接口

- `GET /role/list` - 获取角色列表
- `POST /role/create` - 创建角色
- `PUT /role/update/{id}` - 更新角色
- `DELETE /role/delete/{id}` - 删除角色

### 用户管理接口

- `GET /user/list` - 获取用户列表
- `POST /user/create` - 创建用户
- `PUT /user/update/{id}` - 更新用户
- `DELETE /user/delete/{id}` - 删除用户

### 用户角色管理接口

- `GET /user-role/user/{userId}/roles` - 获取用户角色
- `GET /user-role/user/{userId}/permissions` - 获取用户权限
- `POST /user-role/assign` - 分配用户角色
- `DELETE /user-role/remove` - 移除用户角色

### 测试接口

- `GET /test/public` - 公开接口（无需认证）
- `GET /test/user` - 需要 user 角色
- `GET /test/admin` - 需要 admin 角色
- `GET /test/user-view` - 需要 user:view 权限
- `GET /test/user-manage` - 需要 user:manage 权限

## 权限控制（Spring Security Method Security）

系统使用 Spring Security 的 `@PreAuthorize` 声明式鉴权（角色自动加 `ROLE_` 前缀，权限码原样）：

```java
// 基于角色
@PreAuthorize("hasRole('admin')")
public Result<String> adminOnly() { ... }

// 基于权限
@PreAuthorize("hasAuthority('user:view')")
public Result<String> viewPermission() { ... }

// 多权限任一满足
@PreAuthorize("hasAuthority('user:view') or hasAuthority('role:view')")
public Result<String> anyPermission() { ... }

// 多权限全部满足
@PreAuthorize("hasAuthority('user:view') and hasAuthority('role:view')")
public Result<String> allPermissions() { ... }

// 角色 + 权限组合
@PreAuthorize("hasAuthority('user:create') and hasRole('admin')")
public Result<String> createUser() { ... }
```

### 获取当前登录用户

使用 `@CurrentUser` 注解直接注入当前登录主体：

```java
@GetMapping("/me")
public Result<User> me(@CurrentUser UserPrincipal principal) {
    Long userId = principal.getUserId();
    String username = principal.getUsername();
    // ...
}
```

## 认证流程（双令牌）

1. **登录** `POST /auth/login` → 返回 `accessToken`（默认 2h）+ `refreshToken`（默认 7d）。
2. **业务请求** 携带 `Authorization: Bearer <accessToken>`。权限由 Security 过滤链实时从数据库加载。
3. **令牌过期**：access token 过期返回 401，前端用 refreshToken 调 `POST /auth/refresh` 换取新令牌对（旧 refresh token 立即吊销）。
4. **登出** `POST /auth/logout`：把 access + refresh 的 jti 写入黑名单。

> 令牌中**不再携带权限清单**（避免泄露）；前端通过 `GET /auth/permissions` 单独拉取。
> 所有令牌均含 `jti`（唯一标识），用于精确吊销。

## 数据库初始化

系统支持自动初始化数据库表结构：

```yaml
app:
  database:
    auto-init: true  # 启用自动初始化
```

也可手动执行 SQL 脚本：
- MySQL: `src/main/resources/sql/check_and_init_tables.sql`
- PostgreSQL: `src/main/resources/sql/check_and_init_tables_postgresql.sql`

## 异常处理

系统具备完善的异常处理机制，统一处理以下异常：
- 业务异常 (BusinessException)
- 认证异常 (AuthException)
- 系统异常 (SystemException)
- Java 内置异常 (NullPointerException, ArrayIndexOutOfBoundsException 等)

所有异常都会记录到 `sys_exception_log` 表中，并返回统一格式的错误响应。

## API 文档

启动应用后访问：
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 默认账户

- 用户名: admin
- 密码: admin123

> 仅用于开发/演示，生产环境请立即修改。

## 兼容性提示（前端对接）

- 登录响应字段：`token` → `accessToken` + `refreshToken` + `expiresIn`。
- 请求头不变：`Authorization: Bearer <accessToken>`。
- 401 / 403 现在返回统一 `Result` JSON（`code` / `message` / `timestamp` / `traceId`）。
- 前端需实现 refresh 轮换：收到 401 时调用 `POST /auth/refresh` 换取新令牌对。
- 时间字段在响应中按**对外时区**（默认上海）输出；请求体中的时间按对外时区解释后转 UTC 入库。

## 项目文档

- [RBAC 权限管理说明](RBAC_README.md)
- [数据库切换指南](DATABASE_SWITCH_GUIDE.md)
- [数据库初始化指南](INIT_README.md)
- [异常处理测试指南](EXCEPTION_TESTING_README.md)