---
description: common 共享内核与 config 配置类规范：Result、ErrorCode、BizException、BaseAggregate、校验分组、Sa-Token / MyBatis-Flex / OpenAPI 配置模板
alwaysApply: true
---

# common 共享内核开发规范

## 一、定位与包结构

`common` 是所有层都可以使用的 **共享内核**，替代 easy-DDD 中的 `model` 层。它只放跨层复用、与具体业务无关的代码。

```plain
common/
├── result/            # Result<T>、PageResult<T>
├── exception/         # ErrorCode、CommonErrorCode、BizException、BizAssert
├── enums/             # BaseEnum 接口、跨领域共享枚举（如 YesNoEnum）
├── model/             # BaseAggregate、BaseEntity、PageQuery
├── validation/        # 校验分组 ValidGroup
└── util/              # 无 IO 的静态工具（如 PasswordUtils）
```

**包规范约束**：

- 单一领域专用的枚举、错误码放 `domain/{业务名}/model/enums/`， **不要**塞进 `common`
- `common` 不允许出现 Spring Bean（`@Service`、`@Component`），只能有 POJO、接口、枚举、静态工具
- `common/util` 允许包装第三方 **无状态静态工具**（如 Sa-Token 的 `BCrypt`），这样 `domain` 无需直接 import 技术框架

## 二、依赖关系规范

| 模块             | 是否可依赖 common  | 说明                                                                                                        |
|------------------|--------------------|-------------------------------------------------------------------------------------------------------------|
| `domain`         | 允许               | 使用基类、异常、共享枚举、静态工具                                                                          |
| `application`    | 允许               | 使用 `Result`、`PageResult`、校验分组                                                                       |
| `adaptor`        | 允许               | Controller 返回 `Result<T>`，全局异常处理器使用 `ErrorCode`                                                 |
| `infrastructure` | 允许               | `BasePO` 之外的基类、`BizException`                                                                         |
| `config`         | 允许               | —                                                                                                           |
| `common` 自身    | 禁止依赖任何业务层 | 只能依赖 JDK、Lombok、Jackson 注解、Jakarta Validation 注解、MyBatis-Flex `@EnumValue` 注解、第三方静态工具 |

## 三、统一返回 Result

### 3.1 规范

| 规则项      | 规范                                                                                                             |
|-------------|------------------------------------------------------------------------------------------------------------------|
| 类名        | `Result<T>`，位于 `common/result/`                                                                               |
| 字段        | `code`（String）、`message`（String）、`data`（T）                                                               |
| 成功码      | `CommonErrorCode.SUCCESS`（`"0"`）                                                                               |
| HTTP 状态码 | 统一 200（框架级 404 / 405 除外），业务结果通过 `code` 表达                                                      |
| 使用位置    | 仅 `adaptor/input` 的 Controller 与 `GlobalExceptionHandler` 构造；`application`、`domain` **禁止**返回 `Result` |

### 3.2 代码模板

```java
package com.florian.sun.spring.template.common.result;

/**
 * 统一接口返回结构
 * 仅在 adaptor 层构造，application / domain 禁止使用
 */
@Getter
@ToString
public class Result<T> {

    private final String code;
    private final String message;
    private final T data;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.getCode(), CommonErrorCode.SUCCESS.getMessage(), data);
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return CommonErrorCode.SUCCESS.getCode().equals(code);
    }
}
```

### 3.3 分页返回 PageResult

`PageResult<T>` 是 **跨层**的分页载体：Repository 返回 `PageResult<聚合根>`，Assembler 转成 `PageResult<ResponseDTO>`
，Controller 包在 `Result<PageResult<ResponseDTO>>` 里返回。

```java
package com.florian.sun.spring.template.common.result;

/**
 * 分页结果
 */
@Getter
@ToString
public class PageResult<T> {

    private final long pageNum;
    private final long pageSize;
    private final long total;
    private final List<T> records;

    private PageResult(long pageNum, long pageSize, long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records == null ? List.of() : records;
    }

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> records) {
        return new PageResult<>(pageNum, pageSize, total, records);
    }

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(query.getPageNum(), query.getPageSize(), 0, List.of());
    }

    /** 保留分页信息，仅转换记录类型（Assembler 中使用） */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(pageNum, pageSize, total, records.stream().map(mapper).toList());
    }
}
```

## 四、错误码与异常

### 4.1 ErrorCode 接口

所有错误码枚举实现 `ErrorCode`，`BizException` 与 `Result.fail` 只认这个接口。

```java
package com.florian.sun.spring.template.common.exception;

/**
 * 错误码接口
 * 通用错误码在 CommonErrorCode，领域错误码在 domain/{业务名}/model/enums/{业务名}ErrorCode
 */
public interface ErrorCode {

    String getCode();

    String getMessage();
}
```

### 4.2 CommonErrorCode

```java
package com.florian.sun.spring.template.common.exception;

/**
 * 通用错误码
 * 只放与具体业务无关的错误；业务错误码定义在各领域的 {业务名}ErrorCode 中
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    SUCCESS("0", "成功"),
    PARAM_ERROR("PARAM_ERROR", "参数错误"),
    NOT_LOGIN("NOT_LOGIN", "未登录或登录已过期"),
    NO_PERMISSION("NO_PERMISSION", "无访问权限"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    CONCURRENT_CONFLICT("CONCURRENT_CONFLICT", "数据已被他人修改，请刷新后重试"),
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "外部服务调用失败"),
    SYSTEM_ERROR("SYSTEM_ERROR", "系统异常，请稍后重试"),
    ;

    private final String code;
    private final String message;
}
```

### 4.3 BizException

| 规则项   | 规范                                                                                                   |
|----------|--------------------------------------------------------------------------------------------------------|
| 类名     | `BizException extends RuntimeException`                                                                |
| 构造     | 必须传 `ErrorCode`；可选覆盖 message                                                                   |
| 抛出位置 | 聚合根、实体、DomainService、AppService、RepositoryImpl（技术异常翻译）、AdaptorImpl（第三方异常翻译） |
| 捕获位置 | **只在** `adaptor/common/GlobalExceptionHandler` 统一捕获；业务代码不 catch                            |
| 日志     | 业务异常在全局处理器打 `warn`，不打堆栈；系统异常打 `error` 带堆栈                                     |

```java
package com.florian.sun.spring.template.common.exception;

/**
 * 业务异常
 * 领域层 / 应用层校验失败时抛出，由 GlobalExceptionHandler 统一转换为 Result
 */
@Getter
public class BizException extends RuntimeException {

    private final String code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /** 覆盖默认提示，如拼接具体的业务参数 */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /** 包装底层异常，保留 cause 便于排查 */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }
}
```

### 4.4 BizAssert

领域层校验的标准写法，避免到处写 `if (...) throw new BizException(...)`。

```java
package com.florian.sun.spring.template.common.exception;

/**
 * 业务断言
 * 断言失败抛出 BizException
 */
public final class BizAssert {

    private BizAssert() {
    }

    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BizException(errorCode);
        }
    }

    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BizException(errorCode, message);
        }
    }

    public static void notNull(Object obj, ErrorCode errorCode) {
        isTrue(obj != null, errorCode);
    }

    public static void notEmpty(Collection<?> collection, ErrorCode errorCode) {
        isTrue(collection != null && !collection.isEmpty(), errorCode);
    }

    public static void notBlank(String text, ErrorCode errorCode) {
        isTrue(text != null && !text.isBlank(), errorCode);
    }
}
```

## 五、共享枚举

### 5.1 BaseEnum 接口

所有需要落库或对外输出的枚举实现 `BaseEnum<C>`。MyBatis-Flex 通过 `@EnumValue` 直接识别落库字段，PO 可以直接持有枚举类型，Converter
无需手写枚举转换。

```java
package com.florian.sun.spring.template.common.enums;

/**
 * 枚举基础接口
 * code 用于落库与前后端交互，description 用于展示
 */
public interface BaseEnum<C> {

    C getCode();

    String getDescription();

    /** 按 code 查找枚举，找不到返回 null */
    static <C, E extends Enum<E> & BaseEnum<C>> E of(Class<E> enumClass, C code) {
        if (code == null) {
            return null;
        }
        for (E e : enumClass.getEnumConstants()) {
            if (code.equals(e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
```

### 5.2 枚举模板

```java
package com.florian.sun.spring.template.domain.order.model.enums;

/**
 * 订单状态
 * 领域专用枚举放 domain/{业务名}/model/enums；跨领域共享的放 common/enums
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatusEnum implements BaseEnum<Integer> {

    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消"),
    ;

    /** MyBatis-Flex 落库时使用该字段，PO 可直接持有枚举类型 */
    @EnumValue
    private final Integer code;
    private final String description;

    /** 判断类方法允许放在枚举上 */
    public boolean canCancel() {
        return this == WAIT_PAY;
    }
}
```

> `@EnumValue` 是 `com.mybatisflex.annotation.EnumValue`，属于注解而非运行时 API，允许出现在 domain 枚举上。这是 domain 层对
> MyBatis-Flex 的 **唯一**例外，目的是省掉 Converter 里的枚举互转。若希望 domain 完全零技术依赖，可在 `MyBatisFlexConfig` 中为
> `BaseEnum` 注册统一的 `TypeHandler` 替代。

## 六、领域基类

### 6.1 BaseAggregate

```java
package com.florian.sun.spring.template.common.model;

/**
 * 聚合根基类
 * version 用于乐观锁，由 RepositoryImpl 从 PO 回填，业务代码禁止修改
 */
@Getter
@Setter
public abstract class BaseAggregate<ID> {

    /** 主键，新建时为 null，save 后由 RepositoryImpl 回填 */
    private ID id;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public boolean isNew() {
        return id == null;
    }
}
```

### 6.2 BaseEntity

```java
package com.florian.sun.spring.template.common.model;

/**
 * 实体基类
 * 实体只在聚合内部有意义，相等性由 id 决定
 */
@Getter
@Setter
public abstract class BaseEntity<ID> {

    private ID id;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity<?> that = (BaseEntity<?>) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
```

### 6.3 值对象不需要基类

值对象直接使用 Java `record`，天然不可变、按值相等，命名 `{名词}Value`。详见 `ddd-domain-layer.md` 2.4 节。

### 6.4 Param / Result 不需要基类

`{方法名}Param`、`{方法名}Result` 是普通 `@Data` 类或 `record`，不再要求继承 `BaseParam` / `BaseResult`。

### 6.5 PageQuery

分页查询条件基类。`application/dto/req` 中的分页 RequestDTO 与 `domain/model/param` 中的分页 Query 都继承它。

```java
package com.florian.sun.spring.template.common.model;

/**
 * 分页查询基类
 */
@Getter
@Setter
public abstract class PageQuery {

    private static final long MAX_PAGE_SIZE = 200;

    @Min(value = 1, message = "页码最小为 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为 200")
    private long pageSize = 20;
}
```

> `PageQuery` 上的 `@Min` / `@Max` 是 Jakarta Validation 注解，只在被 RequestDTO 继承并经 Controller `@Valid`
> 触发时生效；domain 的 Query 继承它不会引入任何运行时行为。

## 七、校验分组

同一个 RequestDTO 在新增与修改场景校验规则不同（如修改必须有 `id`）时，使用分组。

```java
package com.florian.sun.spring.template.common.validation;

/**
 * 校验分组
 */
public interface ValidGroup {

    /** 新增 */
    interface Create {
    }

    /** 修改 */
    interface Update {
    }
}
```

使用示例见 `ddd-application-layer.md` 1.6 节。

## 八、静态工具

`common/util` 只放 **无 IO、无状态**的静态方法。允许包装第三方静态工具，使 `domain` 不必直接依赖框架。

```java
package com.florian.sun.spring.template.common.util;

import cn.dev33.satoken.secure.BCrypt;

/**
 * 密码工具
 * 包装 Sa-Token 的 BCrypt，domain 层通过此类校验密码而不直接依赖 Sa-Token
 */
public final class PasswordUtils {

    private PasswordUtils() {
    }

    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
```

```java
package com.florian.sun.spring.template.common.util;

/**
 * 业务编号工具
 * 前缀 + 时间戳 + 随机数，个人项目量级足够；需要严格唯一时改用数据库序列或雪花算法
 */
public final class IdUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private IdUtils() {
    }

    public static String nextNo(String prefix) {
        return prefix + LocalDateTime.now().format(FORMATTER) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
```

**禁止**放入 `common/util` 的东西：需要注入 Bean 的类、访问数据库 / Redis / HTTP 的方法、包含业务判断的方法。

## 九、config 包：Spring 配置类

`config` 与 `common` 同级，只做技术装配， **不含业务逻辑**。三个必备配置类模板如下。

### 9.1 SaTokenConfig

```java
package com.florian.sun.spring.template.config;

/**
 * Sa-Token 配置
 * 注册全局登录拦截器；放行登录、文档、错误页
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final String[] EXCLUDE_PATHS = {
            "/auth/login",
            "/error",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // SaInterceptor 同时开启注解鉴权（@SaCheckLogin / @SaCheckPermission / @SaCheckRole）
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(EXCLUDE_PATHS);
    }
}
```

对应 `application.yaml`：

```yaml
sa-token:
  token-name: satoken          # 请求头名称
  timeout: 2592000             # token 有效期（秒），30 天
  active-timeout: -1           # 不限制最低活跃频率
  is-concurrent: true          # 允许同一账号多端登录
  is-share: false              # 每次登录生成新 token
  token-style: uuid
  is-log: false
  is-read-cookie: false        # 前后端分离，只读 header
```

### 9.2 MyBatisFlexConfig

```java
package com.florian.sun.spring.template.config;

/**
 * MyBatis-Flex 配置
 * 用 markerInterface 限定只扫描继承 BaseMapper 的接口，避免把 MapStruct 的 @Mapper 接口误注册为 MyBatis Mapper
 */
@Configuration
@MapperScan(
        basePackages = "com.florian.sun.spring.template.infrastructure",
        markerInterface = BaseMapper.class
)
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 逻辑删除：正常 0，已删除 1（与 BasePO.deleted 字段配合）
        globalConfig.setNormalValueOfLogicDelete(0);
        globalConfig.setDeletedValueOfLogicDelete(1);
    }
}
```

对应 `application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spring_template?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379

mybatis-flex:
  global-config:
    print-banner: false
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

### 9.3 OpenApiConfig

```java
package com.florian.sun.spring.template.config;

/**
 * SpringDoc 配置
 * 声明 Sa-Token 请求头，方便在 swagger-ui 中直接携带 token 调试
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "satoken";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("spring-template API").version("0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(SECURITY_SCHEME)));
    }
}
```

### 9.4 config 包约束

| 允许                                                                                  | 禁止                     |
|---------------------------------------------------------------------------------------|--------------------------|
| `@Configuration`、`@Bean`、`WebMvcConfigurer`、`MyBatisFlexCustomizer`、`@MapperScan` | 任何 `if` 业务判断       |
| 引用 `infrastructure`、`adaptor` 中的类进行装配                                       | 定义 Controller、Service |
| 读取 `application.yaml` 的 `@ConfigurationProperties`                                 | 直接操作数据库           |
