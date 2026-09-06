---
description: application 应用层开发规范：场景编排、CQRS、事务、RequestDTO/ResponseDTO 与 Validation、MapStruct Assembler、Adaptor 接口定义，四种开发模式
alwaysApply: true
---

# application 层开发规范

## 一、基础规范（所有模式通用）

### 1.1 包结构规范

application 层按业务领域划分顶层包，领域下的子包结构保持不变：

```plain
application/
├── {业务名}/                  # 按业务领域划分（如：order、auth）
│   ├── scenario/              # 场景编排：{聚合根名}AppService、{聚合根名}QueryAppService、{动词}QueryAppService
│   ├── assembler/             # MapStruct Assembler：DTO ↔ Param / Query / 聚合根
│   ├── dto/
│   │   ├── req/               # {方法名}RequestDTO
│   │   ├── res/               # {方法名}ResponseDTO
│   │   └── model/             # 被多个 DTO 复用的 {概念}DTO（如 AddressDTO）
│   └── adaptor/               # Adaptor 接口及其专用返回 DTO（application 定义，adaptor/output 实现）
```

### 1.2 Application 层核心定位

**Application 层是场景编排层，是不稳定的；领域层是核心业务规则层，是稳定的。**

- Application 层按业务场景编排 DomainService、Repository、Adaptor，是需求变更最频繁的地方
- 领域层封装稳定的核心规则，不随场景变化
- 同一个领域方法可被多个 Application 场景复用，场景差异体现在编排逻辑

#### 示例：线上下单 vs 后台补单

领域服务只有一个稳定的 `createOrder`，Application 层有两个场景方法：

```java
package com.florian.sun.spring.template.application.order.scenario;

/**
 * 订单应用服务（写模式）
 * 同一个 orderDomainService.createOrder 被两个场景复用
 */
@Service
@RequiredArgsConstructor
public class OrderAppService {

    private final OrderDomainService orderDomainService;
    private final InventoryAdaptor inventoryAdaptor;
    private final OrderAssembler orderAssembler;

    /**
     * 场景1：线上下单
     * 需要先通过 Adaptor 校验库存，编排较复杂
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponseDTO createOrderOnline(CreateOrderRequestDTO requestDTO) {
        // 1. 前置校验：调用外部库存服务（场景编排，不是领域规则）
        for (OrderItemRequestDTO item : requestDTO.getItems()) {
            StockDTO stock = inventoryAdaptor.queryStock(item.getProductId());
            BizAssert.isTrue(stock.available() >= item.getQuantity(), OrderErrorCode.STOCK_NOT_ENOUGH);
        }

        // 2. DTO → Param，调用稳定的领域方法
        CreateOrderParam param = orderAssembler.toCreateOrderParam(requestDTO);
        OrderAggregate order = orderDomainService.createOrder(param);

        // 3. 聚合根 → ResponseDTO
        return orderAssembler.toCreateOrderResponseDTO(order);
    }

    /**
     * 场景2：后台补单
     * 无需校验库存，直接复用同一个领域方法
     */
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponseDTO createOrderManual(CreateOrderManualRequestDTO requestDTO) {
        CreateOrderParam param = orderAssembler.toCreateOrderParam(requestDTO);
        OrderAggregate order = orderDomainService.createOrder(param);
        return orderAssembler.toCreateOrderResponseDTO(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(CancelOrderRequestDTO requestDTO) {
        orderDomainService.cancelOrder(orderAssembler.toCancelOrderParam(requestDTO));
    }
}
```

**关键理解**：

- `orderDomainService.createOrder()` 是 **稳定的领域方法**
- `createOrderOnline` / `createOrderManual` 是 **不稳定的场景方法**
- 新增"批量导入下单"场景只需在 Application 层加方法，领域层无需改动

### 1.3 CQRS 分层规范（读写分离）

| 类型          | 类名                        | 职责                                                            |
|---------------|-----------------------------|-----------------------------------------------------------------|
| Command（写） | `{聚合根名}AppService`      | 只处理状态变更，方法加 `@Transactional`                         |
| Query（读）   | `{聚合根名}QueryAppService` | 只处理查询，直接调用 Repository / Adaptor，不经过 DomainService |
| 计算          | `{动词}QueryAppService`     | 纯计算 / 规则+计算，一个计算一个类                              |

- 直接写具体类， **不再** interface + Impl，也不再继承 `ApplicationCmdService` / `ApplicationQueryService`
- **禁止交叉调用**：Command 类禁止注入 Query 类；两者都需要的查询逻辑下沉到 Repository

### 1.4 参数与返回值规则

| 规则项   | 规范                                                                                                                                          |
|----------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| 输入参数 | 默认使用 `{方法名}RequestDTO`；**单个 ID 查询**允许直接用 `Long id` 等基础类型；禁止 `Map`                                                    |
| 操作人   | 当前登录用户 ID 由 Controller 读取 `StpUtil` 后放入 RequestDTO 的 `operatorId` 字段（或作为独立参数传入），Application **禁止**调用 `StpUtil` |
| 参数校验 | 格式校验由 RequestDTO 上的 Jakarta Validation 注解声明、Controller `@Valid` 触发；AppService **不写**格式校验                                 |
| 返回值   | `{方法名}ResponseDTO`、`PageResult<XxxResponseDTO>`、`void`；**禁止**返回 `Result<T>`、聚合根、PO                                             |
| 错误处理 | 抛 `BizException`；不 try/catch 领域异常；不返回错误码                                                                                        |

### 1.5 DTO 规范

#### 存放位置

| 类型             | 位置                              | 命名                                                                         |
|------------------|-----------------------------------|------------------------------------------------------------------------------|
| 请求 DTO         | `application/{业务名}/dto/req/`   | `{方法名}RequestDTO`                                                         |
| 响应 DTO         | `application/{业务名}/dto/res/`   | `{方法名}ResponseDTO`；列表项可命名 `{名词}ItemResponseDTO`                  |
| 复用 DTO         | `application/{业务名}/dto/model/` | `{概念}DTO`，被多个 Request/Response 引用（如 `AddressDTO`、`OrderItemDTO`） |
| Adaptor 返回 DTO | `application/{业务名}/adaptor/`   | `{概念}DTO`，仅含 Application 需要的字段                                     |

#### 设计原则

- 每个方法一个 RequestDTO，避免多个接口共用一个"大而全"的 DTO
- 复用 DTO 出现明显冗余字段时应拆分，避免入参膨胀、出参泄漏
- RequestDTO 用 `@Data` 类（Controller 需要 set `operatorId`）；ResponseDTO 用 `@Data` 类或 `record`
- DTO 允许出现 Jakarta Validation 注解、SpringDoc `@Schema`、Jackson 注解； **禁止**出现 MyBatis-Flex 注解
- DTO 可以直接使用领域枚举类型（application 依赖 domain），Jackson 默认输出枚举名；若需要输出 `code`，在枚举的 `getCode()` 上加
  `@JsonValue`

#### 格式校验与业务校验的边界

| 校验类型 | 位置                   | 手段                         | 示例                                         |
|----------|------------------------|------------------------------|----------------------------------------------|
| 格式校验 | RequestDTO             | Jakarta Validation 注解      | 非空、长度、范围、正则、嵌套 `@Valid`        |
| 业务校验 | 聚合根 / DomainService | `BizAssert` + `BizException` | 状态是否允许取消、金额是否一致、是否本人订单 |

判断标准： **不查库、不看状态就能判定的是格式校验**，其余都是业务校验。

#### RequestDTO 模板

```java
package com.florian.sun.spring.template.application.order.dto.req;

/**
 * 创建订单请求
 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequestDTO {

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    @Schema(description = "订单明细")
    private List<OrderItemDTO> items;

    @Valid
    @NotNull(message = "收货地址不能为空")
    @Schema(description = "收货地址")
    private AddressDTO receiver;

    @Size(max = 200, message = "备注最多 200 字")
    @Schema(description = "买家备注")
    private String remark;

    /** 操作人 ID，由 Controller 从 Sa-Token 登录态注入，前端传入无效 */
    @JsonIgnore
    @Schema(hidden = true)
    private Long operatorId;
}
```

```java
package com.florian.sun.spring.template.application.order.dto.model;

/**
 * 订单明细 DTO（复用）
 */
@Data
@Schema(description = "订单明细")
public class OrderItemDTO {

    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量最少为 1")
    private Integer quantity;
}
```

#### 分页 RequestDTO 模板

```java
package com.florian.sun.spring.template.application.order.dto.req;

/**
 * 分页查询我的订单
 * 继承 PageQuery 获得 pageNum / pageSize 及其校验
 */
@Getter
@Setter
@Schema(description = "分页查询我的订单")
public class PageMyOrdersRequestDTO extends PageQuery {

    @Schema(description = "订单状态，不传查全部")
    private OrderStatusEnum status;

    @JsonIgnore
    @Schema(hidden = true)
    private Long operatorId;
}
```

#### ResponseDTO 模板

```java
package com.florian.sun.spring.template.application.order.dto.res;

/**
 * 创建订单响应
 */
@Data
@Schema(description = "创建订单响应")
public class CreateOrderResponseDTO {

    private Long orderId;
    private String orderNo;
    private OrderStatusEnum status;
    private BigDecimal totalAmount;
}
```

### 1.6 校验分组

新增与修改共用一个 RequestDTO 但规则不同时，使用 `common/validation/ValidGroup`：

```java

@Data
public class SaveAddressRequestDTO {

    /** 修改时必填，新增时必须为空 */
    @Null(groups = ValidGroup.Create.class, message = "新增时不能传 id")
    @NotNull(groups = ValidGroup.Update.class, message = "修改时 id 不能为空")
    private Long id;

    @NotBlank(groups = {ValidGroup.Create.class, ValidGroup.Update.class}, message = "收货人不能为空")
    private String receiverName;

    @JsonIgnore
    @Schema(hidden = true)
    private Long operatorId;
}
```

Controller 用 `@Validated(ValidGroup.Create.class)` 指定分组，见 `ddd-adaptor-layer.md` 2.2 节。

> 注意：指定了 `groups` 的约束在 **不带分组**的 `@Valid` 下不会触发。要么所有约束都写 groups，要么都不写。

### 1.7 事务规范

| 规则项                         | 规范                                                                                                                                                                                                               |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 事务边界                       | **只在** `{聚合根名}AppService` 的命令方法上加 `@Transactional(rollbackFor = Exception.class)`                                                                                                                     |
| 查询方法                       | 不加事务；跨多表读一致性要求高时可加 `@Transactional(readOnly = true)`                                                                                                                                             |
| DomainService / RepositoryImpl | **禁止**加 `@Transactional`，避免嵌套事务语义混乱                                                                                                                                                                  |
| 回滚                           | `BizException` 是 `RuntimeException`，任何一层抛出都会回滚；显式声明 `rollbackFor = Exception.class` 兜底 checked 异常                                                                                             |
| 远程调用                       | Adaptor 的 HTTP 调用尽量放在事务方法**开头**做前置校验；耗时长的外部调用（发短信、推送）放在事务提交后（`TransactionSynchronizationManager.registerSynchronization` 或 Spring 事件 `@TransactionalEventListener`） |
| 自调用                         | 同一个类内部方法互调不会经过代理，事务不生效；需要事务的方法必须由 Controller 或其他 Bean 调用                                                                                                                     |
| 禁止                           | 在事务方法内 catch 异常后继续提交（会导致部分写入）                                                                                                                                                                |

### 1.8 依赖管理与行为约束

**允许依赖**：

- `domain`（DomainService、Repository 接口、聚合根、Param、Result、领域枚举、错误码）
- `common`（`PageResult`、`BizAssert`、静态工具）
- 本层定义的 Adaptor 接口
- 单体应用内，Application 可以同时注入 **多个领域**的 DomainService / Repository 进行编排（如订单场景查用户昵称），无需为内部领域包一层
  Adaptor

**禁止依赖**：

- `infrastructure`、`adaptor` 中的实现类（只能依赖接口）
- MyBatis-Flex、Sa-Token `StpUtil`、Spring Web（`HttpServletRequest`）、Redis

**允许的行为**：

- 调用 DomainService、Repository 接口、Adaptor 接口
- 通过 Assembler 做 DTO ↔ 领域对象转换
- **编排流程控制**：根据 Adaptor 返回结果决定是否继续、组装多个来源的数据

**禁止的行为**：

- 包含核心业务规则（状态流转判断、金额计算公式、规则匹配）——放聚合根 / 实体 / DomainService
- 调用聚合根 / 实体的 setter 修改状态（读取属性走 `aggregate.getXxxEntity().getYyy()` 是允许的）
- 直接操作数据库或 HTTP

### 1.9 Assembler 转换类规范

| 规则项   | 规范                                                                                     |
|----------|------------------------------------------------------------------------------------------|
| 类名     | `{聚合根名}Assembler` 或 `{场景}Assembler`，位于 `application/{业务名}/assembler/`       |
| 实现     | MapStruct 接口，`@Mapper(componentModel = "spring")`，构造注入到 AppService              |
| 职责     | RequestDTO → Param / Query；聚合根 / Result → ResponseDTO                                |
| 禁止     | 在 Assembler 中写业务判断；把 Assembler 用于 PO 转换（那是 infrastructure 的 Converter） |
| 命名冲突 | MapStruct 注解叫 `@Mapper`，但类名**必须**以 `Assembler` 结尾，不能叫 `XxxMapper`        |

**MapStruct 注意事项**：

- 目标是 `record`（值对象、record DTO）时 MapStruct 自动使用构造器映射
- 多源参数映射时属性名冲突要显式 `@Mapping(target = "id", source = "aggregate.id")`
- 聚合根的 `isXxx()` / `getXxx()` 会被当成属性；不希望映射到 DTO 同名字段时 `ignore = true`
- 需要 `lombok-mapstruct-binding` 在注解处理器路径上（见 `README.md` 前置准备）

**聚合根 → ResponseDTO 的平铺写法**：聚合根不直接持有属性，属性在根实体上（`aggregate.getOrder().getOrderNo()`）。Assembler 用
`@Mapping(target = ".", source = "order")` 把根实体的所有属性平铺到 DTO，`id` 走聚合根委托的 `getId()`：

- 单源方法的参数命名为 `aggregate`（不要与根实体字段名 `order` / `user` 相同，否则 `source = "order"` 会被解析成参数而不是属性）
- 多源方法用 `source = "aggregate.order"` 指定嵌套路径；多个 `target = "."` 之间不能有同名属性
- 值对象字段（如 `receiver`）按需 `@Mapping(target = "receiver", source = "order.receiver")` 转成对应 DTO

```java
package com.florian.sun.spring.template.application.order.assembler;

/**
 * 订单转换器
 * RequestDTO ↔ Param / Query，聚合根 ↔ ResponseDTO
 */
@Mapper(componentModel = "spring")
public interface OrderAssembler {

    @Mapping(target = "buyerId", source = "operatorId")
    CreateOrderParam toCreateOrderParam(CreateOrderRequestDTO requestDTO);

    @Mapping(target = "buyerId", source = "buyerId")
    CreateOrderParam toCreateOrderParam(CreateOrderManualRequestDTO requestDTO);

    /** 目标为 record，MapStruct 走构造器 */
    OrderItemValue toOrderItemValue(OrderItemDTO dto);

    AddressValue toAddressValue(AddressDTO dto);

    CancelOrderParam toCancelOrderParam(CancelOrderRequestDTO requestDTO);

    @Mapping(target = "buyerId", source = "operatorId")
    PageOrderQuery toPageOrderQuery(PageMyOrdersRequestDTO requestDTO);

    /** 根实体属性平铺到 DTO；orderId 取聚合根委托的 id */
    @Mapping(target = "orderId", source = "id")
    @Mapping(target = ".", source = "order")
    CreateOrderResponseDTO toCreateOrderResponseDTO(OrderAggregate aggregate);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = ".", source = "order")
    OrderItemResponseDTO toOrderItemResponseDTO(OrderAggregate aggregate);

    @Mapping(target = "orderId", source = "aggregate.id")
    @Mapping(target = ".", source = "aggregate.order")
    @Mapping(target = "buyerNickname", source = "buyer.user.nickname")
    @Mapping(target = "logistics", source = "logistics")
    OrderDetailResponseDTO toOrderDetailResponseDTO(OrderAggregate aggregate, UserAggregate buyer, LogisticsInfoDTO logistics);
}
```

### 1.10 Adaptor 接口定义规范

Adaptor 是 Application 对 **进程外**服务的抽象：第三方 HTTP 服务、Sa-Token 会话签发、邮件 / 短信网关等。接口定义在
Application，实现在 `adaptor/{业务名}/output/`。

**最重要的原则：Adaptor 接口必须基于 Application 的业务需要来定义，而不是照搬第三方接口。**

| 规则项   | 规范                                                                                                                           |
|----------|--------------------------------------------------------------------------------------------------------------------------------|
| 接口名   | `{名词}Adaptor`，位于 `application/{业务名}/adaptor/`                                                                          |
| 方法名   | 体现调用方业务意图的动词（`queryStock`、`login`），而不是第三方 API 名                                                         |
| 参数     | 稳定的业务标识用基础类型（`Long productId`）；参数多时直接传 RequestDTO                                                        |
| 返回值   | `{概念}DTO`（record 或 `@Data`），只含 Application 需要的字段；单条不存在返回 `Optional`                                       |
| 错误处理 | 实现类把第三方异常翻译为 `BizException(CommonErrorCode.EXTERNAL_SERVICE_ERROR)` 或领域错误码；Application 不感知第三方异常类型 |
| 归属     | Repository 操作本领域数据库，**不经过** Adaptor；单体内其他领域的数据直接注入其 Repository，**不需要** Adaptor                 |

```java
package com.florian.sun.spring.template.application.order.adaptor;

/**
 * 库存适配器接口
 * 按 Application 需要定义：只关心某商品可用库存
 */
public interface InventoryAdaptor {

    StockDTO queryStock(Long productId);
}

/** 库存信息，只含 Application 需要的字段 */
public record StockDTO(Long productId, int available) {
}
```

```java
package com.florian.sun.spring.template.application.auth.adaptor;

/**
 * 会话适配器接口
 * 抽象登录态的签发与销毁，实现由 Sa-Token 提供（adaptor/auth/output/SaTokenSessionAdaptorImpl）
 */
public interface SessionAdaptor {

    /** 为用户签发登录态，返回 token 信息 */
    SessionDTO login(Long userId);

    /** 注销当前会话 */
    void logout();

    /** 将指定用户的所有会话踢下线（禁用用户时使用） */
    void kickout(Long userId);
}

public record SessionDTO(String tokenName, String tokenValue, long timeoutSeconds) {
}
```

---

## 二、写模式 Application 规范

### 2.1 命名规范

| 规则项   | 规范                                                     | 示例                               |
|----------|----------------------------------------------------------|------------------------------------|
| 类名     | `{聚合根名}AppService`，`@Service` 具体类                | `OrderAppService`                  |
| 方法命名 | **业务动词**，禁止 `save` / `update`                     | `createOrderOnline`、`cancelOrder` |
| 请求 DTO | `{方法名}RequestDTO`                                     | `CreateOrderRequestDTO`            |
| 响应 DTO | `{方法名}ResponseDTO` 或 `void`                          | `CreateOrderResponseDTO`           |
| 事务     | 每个方法 `@Transactional(rollbackFor = Exception.class)` | —                                  |

### 2.2 调用链

**简单场景**（如后台补单）：

```plain
Controller → AppService(@Transactional) → DomainService → Repository
```

**复杂场景**（如线上下单，需调用外部服务前置校验）：

```plain
Controller → AppService(@Transactional) → Adaptor（校验库存）
                                        → DomainService → Repository
```

Application 层在写模式中的职责：

1. 通过 Adaptor 获取外部数据或做前置校验（按场景需要）
2. 通过 Assembler 将 RequestDTO 转为 Param
3. 调用 DomainService（内部完成加载聚合根、执行业务方法、持久化）
4. 通过 Assembler 将聚合根转为 ResponseDTO

### 2.3 完整代码示例

见 1.2 节 `OrderAppService`。

### 2.4 登录场景示例（Sa-Token 会话通过 Adaptor 签发）

```java
package com.florian.sun.spring.template.application.auth.scenario;

/**
 * 认证应用服务
 * 密码校验是领域规则（UserDomainService），token 签发是外部技术（SessionAdaptor）
 */
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserDomainService userDomainService;
    private final SessionAdaptor sessionAdaptor;
    private final AuthAssembler authAssembler;

    public LoginResponseDTO login(LoginRequestDTO requestDTO) {
        // 1. 领域层校验邮箱、密码、账号状态，失败抛 BizException
        UserAggregate user = userDomainService.authenticate(authAssembler.toAuthenticateParam(requestDTO));

        // 2. 通过 Adaptor 签发登录态（实现类内部调用 StpUtil.login）
        SessionDTO session = sessionAdaptor.login(user.getId());

        // 3. 组装响应
        return authAssembler.toLoginResponseDTO(user, session);
    }

    public void logout() {
        sessionAdaptor.logout();
    }
}
```

登录不涉及本领域数据写入，可不加 `@Transactional`；若登录需要记录最后登录时间等写操作，则加上。

---

## 三、读模式 Application 规范

### 3.1 命名规范

| 规则项   | 规范                                                                     | 示例                             |
|----------|--------------------------------------------------------------------------|----------------------------------|
| 类名     | `{聚合根名}QueryAppService`，`@Service` 具体类                           | `OrderQueryAppService`           |
| 方法命名 | `get{名词}Detail`、`page{名词}`、`list{名词}`，避免抽象词（`process`）   | `getOrderDetail`、`pageMyOrders` |
| 参数     | 单 ID 用基础类型；分页 / 多条件用 `{方法名}RequestDTO extends PageQuery` | —                                |
| 返回值   | `{方法名}ResponseDTO`、`PageResult<{名词}ItemResponseDTO>`、`List<...>`  | —                                |

### 3.2 查询数据来源

| 场景         | 数据来源              | 说明                                       |
|--------------|-----------------------|--------------------------------------------|
| 领域内查询   | 本领域 Repository     | 返回聚合根 / Result，通过 Assembler 转 DTO |
| 单体内跨领域 | 其他领域的 Repository | 直接注入，如订单详情补买家昵称             |
| 进程外数据   | Adaptor               | 第三方物流、天气等                         |
| 混合查询     | Repository + Adaptor  | 先查本领域，再补外部数据，组装后返回       |

### 3.3 完整代码示例

```java
package com.florian.sun.spring.template.application.order.scenario;

/**
 * 订单查询应用服务（读模式）
 * 不经过 DomainService，直接调用 Repository / Adaptor
 */
@Service
@RequiredArgsConstructor
public class OrderQueryAppService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final LogisticsAdaptor logisticsAdaptor;
    private final OrderAssembler orderAssembler;

    /** 混合查询：本领域订单 + 用户领域昵称 + 第三方物流 */
    public OrderDetailResponseDTO getOrderDetail(Long orderId, Long operatorId) {
        OrderAggregate order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(OrderErrorCode.ORDER_NOT_FOUND));
        BizAssert.isTrue(order.isOwnedBy(operatorId), OrderErrorCode.NOT_ORDER_OWNER);

        // 属性在根实体上：aggregate.getOrder().getXxx()；判断类方法直接调聚合根
        UserAggregate buyer = userRepository.findById(order.getOrder().getBuyerId())
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));

        LogisticsInfoDTO logistics = order.isPaid()
                ? logisticsAdaptor.queryByOrderNo(order.getOrder().getOrderNo()).orElse(null)
                : null;

        return orderAssembler.toOrderDetailResponseDTO(order, buyer, logistics);
    }

    /** 分页查询：PageResult 通过 map 转换记录类型，分页信息保持不变 */
    public PageResult<OrderItemResponseDTO> pageMyOrders(PageMyOrdersRequestDTO requestDTO) {
        PageOrderQuery query = orderAssembler.toPageOrderQuery(requestDTO);
        return orderRepository.pageByBuyer(query).map(orderAssembler::toOrderItemResponseDTO);
    }
}
```

---

## 四、纯计算模式 Application 规范

### 4.1 核心原则

- **一个计算方法一个类**，职责单一
- 类名以 **动词**开头，不以聚合根命名

### 4.2 命名规范

| 规则项   | 规范                    | 示例                              |
|----------|-------------------------|-----------------------------------|
| 类名     | `{动词}QueryAppService` | `FreightCalculateQueryAppService` |
| 方法命名 | 动词                    | `calculateFreight`                |
| 事务     | 不加                    | —                                 |

### 4.3 调用链

```plain
Controller → {动词}QueryAppService → Adaptor（如需外部数据） → DomainService → Result → Assembler → ResponseDTO
```

### 4.4 完整代码示例

```java
package com.florian.sun.spring.template.application.freight.scenario;

/**
 * 运费试算应用服务（纯计算模式）
 * 一个计算方法一个类
 */
@Service
@RequiredArgsConstructor
public class FreightCalculateQueryAppService {

    private final RegionAdaptor regionAdaptor;
    private final FreightCalculateDomainService freightCalculateDomainService;
    private final FreightAssembler freightAssembler;

    public CalculateFreightResponseDTO calculateFreight(CalculateFreightRequestDTO requestDTO) {
        // 1. 通过 Adaptor 获取外部数据（是否偏远地区）
        boolean remoteArea = regionAdaptor.isRemoteArea(requestDTO.getProvince(), requestDTO.getCity());

        // 2. 组装 Param，调用 DomainService 计算
        CalculateFreightParam param = freightAssembler.toParam(requestDTO, remoteArea);
        CalculateFreightResult result = freightCalculateDomainService.calculateFreight(param);

        // 3. Result → ResponseDTO
        return freightAssembler.toResponseDTO(result);
    }
}
```

---

## 五、规则+计算模式 Application 规范

### 5.1 核心原则

同纯计算模式： **一个计算方法一个类**，类名以动词开头。

### 5.2 命名规范

| 规则项   | 规范                    | 示例                                  |
|----------|-------------------------|---------------------------------------|
| 类名     | `{动词}QueryAppService` | `DiscountPreCalculateQueryAppService` |
| 方法命名 | 动词                    | `preCalculateDiscount`                |

### 5.3 调用链

```plain
Controller → {动词}QueryAppService → DomainService → Repository 加载规则聚合根
                                                   → 聚合根.match / calculate → Result → ResponseDTO
```

### 5.4 完整代码示例

```java
package com.florian.sun.spring.template.application.coupon.scenario;

/**
 * 优惠预计算应用服务（规则+计算模式）
 */
@Service
@RequiredArgsConstructor
public class DiscountPreCalculateQueryAppService {

    private final UserRepository userRepository;
    private final DiscountCalculateDomainService discountCalculateDomainService;
    private final DiscountAssembler discountAssembler;

    public PreCalculateDiscountResponseDTO preCalculateDiscount(PreCalculateDiscountRequestDTO requestDTO) {
        // 1. 单体内跨领域：直接查用户领域拿会员等级
        UserAggregate user = userRepository.findById(requestDTO.getOperatorId())
                .orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));

        // 2. 组装 Param，DomainService 加载规则并计算
        CalculateDiscountParam param = discountAssembler.toParam(requestDTO, user.getUser().getMemberLevel());
        CalculateDiscountResult result = discountCalculateDomainService.calculateDiscount(param);

        // 3. Result → ResponseDTO
        return discountAssembler.toResponseDTO(result);
    }
}
```
