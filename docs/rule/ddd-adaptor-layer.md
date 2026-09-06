---
description: adaptor 适配器层开发规范：Controller（Sa-Token 鉴权、Validation、SpringDoc）、定时任务、Output Adaptor 实现、GlobalExceptionHandler，四种开发模式
alwaysApply: true
---

# adaptor 层开发规范

## 一、基础规范（所有模式通用）

### 1.1 角色定位

- **防腐层（ACL）**：隔离外部技术细节（HTTP 协议、第三方服务、鉴权框架），防止污染领域层和应用层
- **双向适配**：
    - **Input Adaptor**：将外部请求（HTTP、定时触发、事件）转换为 Application 调用
    - **Output Adaptor**：实现 Application 定义的 Adaptor 接口，把业务语义的调用转换为第三方协议 / 框架 API

### 1.2 包结构规范

```plain
adaptor/
├── common/                    # 跨业务的 Web 组件：GlobalExceptionHandler、日志切面
└── {业务名}/                  # 按业务领域划分（如：order、auth）
    ├── input/                 # 入口：{业务名}Controller、{业务名}Job、{业务名}EventListener
    └── output/                # 出口：{名词}AdaptorImpl
        ├── client/            # 第三方接口的请求 / 响应类（可选）
        └── converter/         # 第三方响应 → Application DTO 的 Converter（可选）
```

### 1.3 分层与职责

| 包路径                     | 职责                                                                      | 依赖关系                                                                |
|----------------------------|---------------------------------------------------------------------------|-------------------------------------------------------------------------|
| `adaptor/{业务名}/input/`  | 接收外部请求，读取登录态，触发参数校验，调用 AppService，包装 `Result<T>` | 只依赖 `application`、`common`；**禁止**依赖 `domain`、`infrastructure` |
| `adaptor/{业务名}/output/` | 实现 `application/{业务名}/adaptor/` 中定义的接口，封装第三方 / 框架调用  | 依赖 `application`（接口与 DTO）、`common`；**禁止**依赖 `domain`       |
| `adaptor/common/`          | 全局异常处理、通用切面                                                    | 依赖 `common`                                                           |

### 1.4 Adaptor 接口定义核心原则

**Adaptor 接口必须基于 Application 层的业务需要来定义，而不是根据第三方接口来定义。** 接口定义规范见
`ddd-application-layer.md` 1.10 节，本层只负责实现。

- 实现类内部可以 **组合调用多个第三方接口**，对 Application 透明
- 第三方请求 / 响应格式的差异由实现类内部的 Converter 处理，不暴露给 Application
- 第三方接口变化时只改实现类，Application 不受影响

### 1.5 实现约束

- 所有 Adaptor 实现类位于 `adaptor/{业务名}/output/`，与接口定义分离
- **禁止**在 Adaptor 中编写业务逻辑，仅做协议转换与异常翻译
- 第三方异常必须翻译为 `BizException`（`CommonErrorCode.EXTERNAL_SERVICE_ERROR` 或具体领域错误码）， **禁止**把
  `RestClientException` 等技术异常抛给 Application
- 第三方返回 `null` / 404 等"不存在"语义时，接口返回 `Optional.empty()`，不抛异常

### 1.6 Adaptor 层允许使用设计模式做技术适配

与领域层 **禁止使用设计模式**不同（详见 `ddd-domain-layer.md` 1.3 节），Adaptor 层 **允许**使用策略、路由等模式处理技术适配。

| 场景                               | 做法                                            |
|------------------------------------|-------------------------------------------------|
| 按渠道 ID 路由不同的第三方接口     | Output Adaptor 实现类中 `switch` 或策略模式路由 |
| 按供应商类型调用不同的下单接口     | 实现类中按类型选择不同的 Client                 |
| 同一能力有 HTTP / SDK 两种接入方式 | 实现类中按配置选择                              |

| 维度         | 领域层（Domain）               | 适配器层（Adaptor）            |
|--------------|--------------------------------|--------------------------------|
| **设计模式** | 禁止                           | 允许                           |
| **分支依据** | 业务规则（订单类型、会员等级） | 技术维度（渠道、供应商、协议） |
| **核心区别** | 业务逻辑必须内聚可见           | 技术细节可以隔离封装           |

### 1.7 调用链控制

**流程1**（纯领域操作）：

```plain
input adaptor → application → domain → infrastructure（本领域数据库）
```

**流程2**（需要外部数据后再执行领域逻辑）：

```plain
input adaptor → application → output adaptor（获取外部数据 / 前置校验）
                            → domain → infrastructure
```

**流程3**（纯外部调用）：

```plain
input adaptor → application → output adaptor
```

### 1.8 命名规则

| 类型                | 命名                                        | 示例                                                |
|---------------------|---------------------------------------------|-----------------------------------------------------|
| Controller          | `{业务名}Controller`                        | `OrderController`、`AuthController`                 |
| 定时任务            | `{业务名}Job`                               | `OrderJob`                                          |
| 事件监听            | `{业务名}EventListener`                     | `OrderEventListener`                                |
| Output Adaptor 实现 | `{名词}AdaptorImpl`；有多个实现时加技术前缀 | `InventoryAdaptorImpl`、`SaTokenSessionAdaptorImpl` |
| 第三方响应类        | `{第三方}{名词}Response`                    | `InventoryApiResponse`                              |
| 转换类              | `{名词}Converter`                           | `InventoryConverter`                                |
| 方法                | 动词                                        | `queryStock`、`login`                               |

### 1.9 Input Adaptor 入口类型

| 入口类型   | 说明                                                    | 类命名                  | 注意                                                                |
|------------|---------------------------------------------------------|-------------------------|---------------------------------------------------------------------|
| Controller | HTTP 接口（Spring MVC）                                 | `{业务名}Controller`    | 见 1.10 节                                                          |
| 定时任务   | Spring `@Scheduled`                                     | `{业务名}Job`           | `config` 中需 `@EnableScheduling`；方法体只调用一个 AppService 方法 |
| 事件监听   | Spring `@EventListener` / `@TransactionalEventListener` | `{业务名}EventListener` | 用于事务提交后的异步动作（发通知等）                                |

**通用规则**：

- 所有入口职责相同：接收请求、转换参数、调用 Application 层
- **禁止**在 Input Adaptor 中编写业务逻辑、调用 Repository、拼装领域对象
- 不同模式调用的 AppService 不同（见各模式章节）

### 1.10 Controller 通用规范

| 规则项              | 规范                                                                                                                              |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| 注解                | `@RestController` + `@RequestMapping("/{资源复数}")` + `@RequiredArgsConstructor`                                                 |
| 路径风格            | RESTful 资源路径；状态变更用 `POST /{资源}/{id}/{动作}`（如 `/orders/{id}/cancel`）                                               |
| 返回值              | 一律 `Result<T>`；无数据用 `Result<Void>` + `Result.success()`                                                                    |
| 参数校验            | `@RequestBody` 前加 `@Valid`；分组校验用 `@Validated(ValidGroup.Create.class)`；GET 查询对象用 `@Valid @ParameterObject`          |
| 路径 / 查询参数校验 | 类上加 `@Validated`，参数上直接写约束（`@Min(1) Long id`）                                                                        |
| 登录态              | 全局拦截器已要求登录；操作人 ID 用 `StpUtil.getLoginIdAsLong()` 读取后 set 到 RequestDTO 的 `operatorId`                          |
| 鉴权                | 需要权限的方法加 `@SaCheckPermission("资源:动作")`；需要角色加 `@SaCheckRole`；无需登录的加 `@SaIgnore` 并在 `SaTokenConfig` 放行 |
| 文档                | 类上 `@Tag(name = ...)`，方法上 `@Operation(summary = ...)`                                                                       |
| 禁止                | 写 `if` 业务判断、调用 Repository、try/catch、返回聚合根 / PO、注入多个不相关领域的 AppService                                    |

**Sa-Token 使用边界**：

| API                                                                | 允许位置                                        |
|--------------------------------------------------------------------|-------------------------------------------------|
| `StpUtil.getLoginIdAsLong()`、`StpUtil.isLogin()`                  | Controller                                      |
| `@SaCheckLogin`、`@SaCheckPermission`、`@SaCheckRole`、`@SaIgnore` | Controller                                      |
| `StpUtil.login()`、`StpUtil.logout()`、`StpUtil.getTokenInfo()`    | `adaptor/auth/output/SaTokenSessionAdaptorImpl` |
| `StpInterface` 实现                                                | `infrastructure/auth/StpInterfaceImpl`          |
| `SaInterceptor` 注册                                               | `config/SaTokenConfig`                          |
| 其他任何层                                                         | **禁止**                                        |

### 1.11 GlobalExceptionHandler

位于 `adaptor/common/`，是 **唯一**允许把异常转换成 `Result` 的地方。

```java
package com.florian.sun.spring.template.adaptor.common;

/**
 * 全局异常处理
 * 业务异常 warn 不打堆栈；系统异常 error 打堆栈
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 领域 / 应用 / 适配器抛出的业务异常 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常 code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(CommonErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** RequestParam / @PathVariable / @ModelAttribute 参数校验失败 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(CommonErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 请求体不可读、缺参数、类型不匹配 */
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public Result<Void> handleBadRequest(Exception e) {
        log.warn("请求参数错误: {}", e.getMessage());
        return Result.fail(CommonErrorCode.PARAM_ERROR);
    }

    /** Sa-Token：未登录 */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(CommonErrorCode.NOT_LOGIN);
    }

    /** Sa-Token：无权限 / 无角色 */
    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handleNoPermission(SaTokenException e) {
        log.warn("鉴权失败: {}", e.getMessage());
        return Result.fail(CommonErrorCode.NO_PERMISSION);
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e) {
        log.error("系统异常", e);
        return Result.fail(CommonErrorCode.SYSTEM_ERROR);
    }
}
```

> 逻辑删除、乐观锁冲突等数据库层面的异常已在 RepositoryImpl 翻译成 `BizException`，这里不需要单独处理
> `DataAccessException`；未翻译的 SQL 异常走兜底分支。

---

## 二、写模式 adaptor 规范

### 2.1 调用链

```plain
Controller → {聚合根名}AppService(@Transactional) → DomainService → Repository
```

### 2.2 Input Adaptor 规范

写模式 Controller 调用 `{聚合根名}AppService`。

```java
package com.florian.sun.spring.template.adaptor.order.input;

/**
 * 订单接口
 * 只做参数校验、登录态读取、调用 AppService、包装 Result
 */
@Tag(name = "订单")
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderAppService orderAppService;
    private final OrderQueryAppService orderQueryAppService;

    @Operation(summary = "创建订单")
    @SaCheckPermission("order:create")
    @PostMapping
    public Result<CreateOrderResponseDTO> createOrder(@Valid @RequestBody CreateOrderRequestDTO requestDTO) {
        requestDTO.setOperatorId(StpUtil.getLoginIdAsLong());
        return Result.success(orderAppService.createOrderOnline(requestDTO));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable @Min(1) Long orderId,
                                    @Valid @RequestBody CancelOrderRequestDTO requestDTO) {
        requestDTO.setOrderId(orderId);
        requestDTO.setOperatorId(StpUtil.getLoginIdAsLong());
        orderAppService.cancelOrder(requestDTO);
        return Result.success();
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderDetailResponseDTO> getOrderDetail(@PathVariable @Min(1) Long orderId) {
        return Result.success(orderQueryAppService.getOrderDetail(orderId, StpUtil.getLoginIdAsLong()));
    }

    @Operation(summary = "我的订单分页")
    @GetMapping
    public Result<PageResult<OrderItemResponseDTO>> pageMyOrders(@Valid @ParameterObject PageMyOrdersRequestDTO requestDTO) {
        requestDTO.setOperatorId(StpUtil.getLoginIdAsLong());
        return Result.success(orderQueryAppService.pageMyOrders(requestDTO));
    }
}
```

> 同一个 Controller 同时注入 `OrderAppService` 与 `OrderQueryAppService` 是允许的：Controller 按 **资源**组织，AppService
> 按 **读写**组织。

#### 分组校验示例

```java

@Tag(name = "收货地址")
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressAppService addressAppService;

    @Operation(summary = "新增收货地址")
    @PostMapping
    public Result<Long> createAddress(@Validated(ValidGroup.Create.class) @RequestBody SaveAddressRequestDTO requestDTO) {
        requestDTO.setOperatorId(StpUtil.getLoginIdAsLong());
        return Result.success(addressAppService.createAddress(requestDTO));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping
    public Result<Void> updateAddress(@Validated(ValidGroup.Update.class) @RequestBody SaveAddressRequestDTO requestDTO) {
        requestDTO.setOperatorId(StpUtil.getLoginIdAsLong());
        addressAppService.updateAddress(requestDTO);
        return Result.success();
    }
}
```

#### 登录接口示例

```java
package com.florian.sun.spring.template.adaptor.auth.input;

/**
 * 认证接口
 * /auth/login 已在 SaTokenConfig 中放行
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    @Operation(summary = "登录")
    @SaIgnore
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO requestDTO) {
        return Result.success(authAppService.login(requestDTO));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authAppService.logout();
        return Result.success();
    }
}
```

#### 定时任务示例

```java
package com.florian.sun.spring.template.adaptor.order.input;

/**
 * 订单定时任务
 * 方法体只调用一个 AppService 方法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderJob {

    private final OrderAppService orderAppService;

    /** 每 5 分钟关闭超时未支付订单 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void closeTimeoutOrders() {
        int closed = orderAppService.closeTimeoutOrders();
        log.info("关闭超时订单 {} 笔", closed);
    }
}
```

### 2.3 Output Adaptor 规范

写模式的核心是本领域状态变更，但写操作前可能需要通过 Output Adaptor 做前置校验（校验库存、验价），这属于 Application 的场景编排。

#### 第三方 HTTP 服务实现示例（RestClient）

```java
package com.florian.sun.spring.template.adaptor.order.output;

/**
 * 库存适配器实现
 * 封装第三方库存服务 HTTP 调用，第三方接口变化只改这里
 */
@Slf4j
@Component
public class InventoryAdaptorImpl implements InventoryAdaptor {

    private final RestClient restClient;

    public InventoryAdaptorImpl(RestClient.Builder builder,
                                @Value("${external.inventory.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public StockDTO queryStock(Long productId) {
        try {
            InventoryApiResponse response = restClient.get()
                    .uri("/api/v1/stock/{productId}", productId)
                    .retrieve()
                    .body(InventoryApiResponse.class);
            BizAssert.notNull(response, CommonErrorCode.EXTERNAL_SERVICE_ERROR);
            // 只回传 Application 需要的字段（防腐裁剪）
            return InventoryConverter.toStockDTO(response);
        } catch (RestClientException e) {
            log.error("查询库存失败, productId: {}", productId, e);
            throw new BizException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
```

```java
package com.florian.sun.spring.template.adaptor.order.output.client;

/** 第三方库存接口原始响应，仅在 adaptor 内部流转 */
@Data
public class InventoryApiResponse {
    private Long skuId;
    private Integer availableQty;
    private Integer lockedQty;
    private String warehouseCode;
}
```

```java
package com.florian.sun.spring.template.adaptor.order.output.converter;

/** 第三方响应 → Application DTO */
public final class InventoryConverter {

    private InventoryConverter() {
    }

    public static StockDTO toStockDTO(InventoryApiResponse response) {
        return new StockDTO(response.getSkuId(), response.getAvailableQty());
    }
}
```

#### Sa-Token 会话适配器实现示例

```java
package com.florian.sun.spring.template.adaptor.auth.output;

/**
 * 基于 Sa-Token 的会话适配器
 * Application 只依赖 SessionAdaptor 接口，换鉴权框架只改这里
 */
@Component
public class SaTokenSessionAdaptorImpl implements SessionAdaptor {

    @Override
    public SessionDTO login(Long userId) {
        StpUtil.login(userId);
        SaTokenInfo info = StpUtil.getTokenInfo();
        return new SessionDTO(info.getTokenName(), info.getTokenValue(), info.getTokenTimeout());
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void kickout(Long userId) {
        StpUtil.kickout(userId);
    }
}
```

---

## 三、读模式 adaptor 规范

### 3.1 调用链

**领域内查询：**

```plain
Controller → {聚合根名}QueryAppService → Repository
```

**跨进程查询：**

```plain
Controller → {聚合根名}QueryAppService → Output Adaptor
```

### 3.2 Input Adaptor 规范

读模式 Controller 调用 `{聚合根名}QueryAppService`，使用 `GET`；分页 / 多条件查询对象用 `@Valid @ParameterObject`
绑定查询参数。示例见 2.2 节 `getOrderDetail`、`pageMyOrders`。

### 3.3 Output Adaptor 规范

#### 接口定义（application 层）

```java
package com.florian.sun.spring.template.application.order.adaptor;

/**
 * 物流适配器接口
 * 参数用稳定业务标识（订单号），"不存在"用 Optional 表达
 */
public interface LogisticsAdaptor {

    Optional<LogisticsInfoDTO> queryByOrderNo(String orderNo);
}

public record LogisticsInfoDTO(String carrier, String trackingNo, String latestStatus) {
}
```

#### 实现（adaptor 层）

```java
package com.florian.sun.spring.template.adaptor.order.output;

/**
 * 物流适配器实现
 */
@Slf4j
@Component
public class LogisticsAdaptorImpl implements LogisticsAdaptor {

    private final RestClient restClient;

    public LogisticsAdaptorImpl(RestClient.Builder builder,
                                @Value("${external.logistics.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<LogisticsInfoDTO> queryByOrderNo(String orderNo) {
        try {
            LogisticsApiResponse response = restClient.get()
                    .uri("/track?orderNo={orderNo}", orderNo)
                    .retrieve()
                    .body(LogisticsApiResponse.class);
            return Optional.ofNullable(response).map(LogisticsConverter::toLogisticsInfoDTO);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("查询物流失败, orderNo: {}", orderNo, e);
            throw new BizException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
```

---

## 四、纯计算模式 adaptor 规范

### 4.1 调用链

```plain
Controller → {动词}QueryAppService → DomainService → 计算并返回 Result
```

### 4.2 Input Adaptor 规范

纯计算模式 Controller 调用 `{动词}QueryAppService`。计算类接口若参数复杂使用 `POST` + `@RequestBody`；参数简单用 `GET`。

```java

@Tag(name = "运费")
@RestController
@RequestMapping("/freight")
@RequiredArgsConstructor
public class FreightController {

    private final FreightCalculateQueryAppService freightCalculateQueryAppService;

    @Operation(summary = "运费试算")
    @PostMapping("/calculate")
    public Result<CalculateFreightResponseDTO> calculateFreight(@Valid @RequestBody CalculateFreightRequestDTO requestDTO) {
        return Result.success(freightCalculateQueryAppService.calculateFreight(requestDTO));
    }
}
```

### 4.3 Output Adaptor 规范

纯计算模式通常不需要 Output Adaptor；当计算需要进程外数据（汇率、地区信息）时通过 Adaptor 获取，实现方式同 2.3 / 3.3 节。

---

## 五、规则+计算模式 adaptor 规范

### 5.1 调用链

```plain
Controller → {动词}QueryAppService → DomainService → Repository 加载规则聚合根
                                                   → 聚合根.match / calculate → Result
```

### 5.2 Input Adaptor 规范

同纯计算模式，Controller 调用 `{动词}QueryAppService`，示例：

```java

@Operation(summary = "优惠预计算")
@PostMapping("/discount/pre-calculate")
public Result<PreCalculateDiscountResponseDTO> preCalculateDiscount(@Valid @RequestBody PreCalculateDiscountRequestDTO requestDTO) {
    requestDTO.setOperatorId(StpUtil.getLoginIdAsLong());
    return Result.success(discountPreCalculateQueryAppService.preCalculateDiscount(requestDTO));
}
```

### 5.3 Output Adaptor 规范

规则数据由 Repository 从数据库加载，通常不需要 Output Adaptor；需要进程外数据时同 2.3 / 3.3 节。
