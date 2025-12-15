# Java 安全管理系统

基于 Spring Boot + MyBatis Plus + JWT 的安全管理框架，支持 RBAC 权限控制和多数据库。

## 核心功能

- 用户认证与授权 (JWT)
- RBAC 权限控制系统（角色-权限-用户三维管理）
- 多数据库支持（MySQL 和 PostgreSQL）
- 完善的异常处理机制
- 自动数据库表初始化
- RESTful API 设计
- Swagger API 文档

## 技术栈

- Spring Boot 3.0.2
- MyBatis Plus 3.5.3.1
- MySQL / PostgreSQL
- JWT
- Lombok
- Hutool
- SpringDoc OpenAPI (Swagger)
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

- JDK 8+
- Maven 3.6+
- MySQL 5.7+ 或 PostgreSQL 10+
- IDE (IntelliJ IDEA 推荐)

### 2. 数据库配置

在 `src/main/resources/application.yml` 中配置数据库连接：

```yaml
spring:
  profiles:
    active: mysql  # 或 postgresql
```

修改对应数据库配置文件：
- MySQL: `application-mysql.yml`
- PostgreSQL: `application-postgresql.yml`

### 3. 启动应用

```bash
mvn spring-boot:run
```

系统会自动初始化数据库表结构和基础数据。

## API 接口

### 认证接口

- `POST /auth/login` - 用户登录
- `POST /auth/register` - 用户注册
- `GET /auth/info` - 获取当前用户信息

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

## 权限控制

系统提供基于注解的权限控制机制：

```java
// 基于角色的访问控制
@RequirePermission(roles = {"admin"})
public Result<String> adminOnly() { ... }

// 基于权限的访问控制
@RequirePermission(value = {"user:view"})
public Result<String> viewPermission() { ... }

// 多权限任一满足
@RequirePermission(value = {"user:view", "role:view"}, logical = Logical.ANY)
public Result<String> anyPermission() { ... }

// 多权限全部满足
@RequirePermission(value = {"user:view", "role:view"}, logical = Logical.ALL)
public Result<String> allPermissions() { ... }
```

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
- 密码: 123456

## 项目文档

- [RBAC 权限管理说明](RBAC_README.md)
- [数据库切换指南](DATABASE_SWITCH_GUIDE.md)
- [数据库初始化指南](INIT_README.md)
- [异常处理测试指南](EXCEPTION_TESTING_README.md)