---
description: domain 领域层开发规范：聚合根、实体、值对象、Param/Result、领域服务、仓储接口、错误码，四种开发模式
alwaysApply: true
---

# domain 层开发规范

## 一、基础规范（所有模式通用）

### 1.1 包结构规范

domain 层按业务领域划分顶层包，领域下的子包结构保持不变：

```plain
domain/
├── {业务名}/                  # 按业务领域划分（如：order、user、coupon）
│   ├── model/
│   │   ├── aggregate/         # 聚合根 {名词}Aggregate：只持有根实体、子实体、值对象
│   │   ├── entity/            # 实体 {名词}Entity：根实体（对应主表）与子实体（对应子表），持有属性
│   │   ├── value/             # 值对象 {名词}Value（record）
│   │   ├── param/             # 方法入参 {方法名}Param、仓储查询条件 {方法名}Query
│   │   ├── result/            # 领域计算返回 {方法名}Result
│   │   ├── enums/             # 领域枚举 {名词}Enum、错误码 {业务名}ErrorCode
│   │   └── event/             # 领域事件 {名词}{动作}Event（record，可选）
│   ├── service/               # 领域服务 {聚合根名}DomainService / {动词}DomainService
│   └── repository/            # 仓储接口 {聚合根名}Repository
```

**包规范约束**：domain 层只能包含上述类型的代码（聚合根、实体、值对象、Param、Query、Result、领域枚举、错误码、领域事件、DomainService、Repository
接口），禁止出现 Utils、常量类、配置类、DTO、PO。

### 1.2 领域层隔离性规则

- domain 层仅包含纯业务代码， **禁止引用技术框架**：MyBatis-Flex 运行时 API、Sa-Token（`StpUtil` 等）、MapStruct、Jakarta
  Validation、Spring Web、Redis、Jackson
- **允许**：JDK、Lombok、Spring 的 `@Service` / `@Component` 注解与构造注入、`common` 包（基类、`BizException`、`BizAssert`
  、静态工具）、Hutool 的非 IO 工具、领域枚举上的 MyBatis-Flex `@EnumValue` 注解
- 通用工具直接调用 `cn.hutool.*`，不增加转发封装；Hutool 的 HTTP、文件、数据库等 IO API 仍禁止在 domain 中使用
- Repository 接口定义在 domain 层，实现在 infrastructure 层
- Repository 管理 **领域内**的数据库、缓存操作，不依赖 Adaptor，不调用第三方服务
- 当前登录用户 ID 等上下文信息由外层通过 Param 显式传入，domain **禁止**调用 `StpUtil.getLoginId()`

```java
// ✅ 正确：操作人由 Param 传入
public void cancel(CancelOrderParam param) {
    BizAssert.isTrue(Objects.equals(this.buyerId, param.getOperatorId()), OrderErrorCode.NOT_ORDER_OWNER);
    
}

// ❌ 错误：领域层读取登录上下文
public void cancel(CancelOrderParam param) {
    Long operatorId = StpUtil.getLoginIdAsLong(); // 依赖 Sa-Token，单元测试无法运行
    
}
```

### 1.3 领域层禁止使用设计模式

**禁止在领域层使用设计模式**，包括策略模式、工厂模式（静态工厂方法 `create` 不算）、模板方法、责任链、状态模式等。

#### 禁止原因

设计模式是 **技术手段**而非 **业务语义**，在领域层引入会导致：

- **业务逻辑碎片化**：核心规则分散在多个 Strategy / Handler 中，无法在一个方法内看清完整规则
- **过度抽象**：为适配模式接口而强行拆分逻辑，破坏业务内聚
- **违反领域层定位**：领域层应直接表达业务规则

#### 核心原则

**个人项目的业务分支是可穷举的，不需要为扩展性引入设计模式。** 领域内的分支（如订单类型、会员等级、支付方式）数量有限，直接用
`if/else`、`switch`（推荐 Java 21+ 的 switch 表达式与模式匹配）在 DomainService、聚合根、实体、值对象中处理即可。

| 场景                   | 正确做法                      | 错误做法                             |
|------------------------|-------------------------------|--------------------------------------|
| 不同会员等级的折扣计算 | 聚合根方法内 `switch (level)` | `DiscountStrategy` 接口 + 多个实现类 |
| 不同订单状态下的校验   | 聚合根方法内用状态枚举判断    | 状态模式 + 多个 State 类             |
| 不同支付方式的手续费   | DomainService 内 `if/else`    | `FeeCalculator` 工厂                 |

```java
// ✅ 正确：业务逻辑直接写在聚合根中，一目了然
public BigDecimal calculateDiscount(MemberLevelEnum level) {
    return switch (level) {
        case NORMAL -> BigDecimal.ZERO;
        case VIP -> totalAmount.multiply(new BigDecimal("0.05"));
        case SVIP -> totalAmount.multiply(new BigDecimal("0.10"));
    };
}

// ❌ 错误：引入策略模式，逻辑分散在多个类
public BigDecimal calculateDiscount(MemberLevelEnum level) {
    return discountStrategyFactory.get(level).calculate(this);
}
```

#### 设计模式的正确归属

设计模式 **允许在 adaptor 层使用**，用于技术路由（如按渠道选择不同第三方接口）。详见 `ddd-adaptor-layer.md` 1.6 节。

### 1.4 Param / Query 对象规范

| 规则项     | 规范                                                                 |
|------------|----------------------------------------------------------------------|
| Param 类名 | `{方法名}Param`，DomainService / 聚合根 / 实体方法的入参             |
| Query 类名 | `{方法名}Query`，Repository 查询方法的入参；分页查询继承 `PageQuery` |
| 实现方式   | `@Data` 类或 `record`；无需继承基类                                  |
| 存放位置   | `domain/{业务名}/model/param/`                                       |
| 禁止       | 出现 Jakarta Validation 注解、`@Schema` 等 DTO 注解                  |

```java
/**
 * 确认支付参数
 */
@Data
public class ConfirmPaymentParam {

    private Long orderId;
    private BigDecimal paidAmount;
    private String paymentChannel;
    private LocalDateTime payTime;
}

/**
 * 分页查询买家订单条件
 */
@Getter
@Setter
public class PageOrderQuery extends PageQuery {

    private Long buyerId;
    private OrderStatusEnum status;
}
```

### 1.5 Result 对象规范

| 规则项   | 规范                                                                          |
|----------|-------------------------------------------------------------------------------|
| 类名     | `{方法名}Result`                                                              |
| 实现方式 | `@Data` 类或 `record`；可以有充血方法                                         |
| 使用场景 | DomainService 计算方法的返回值；Repository 返回与聚合根结构差异较大的查询投影 |

```java
/**
 * 运费计算结果
 */
@Data
public class CalculateFreightResult {

    private BigDecimal freight;
    private boolean freeShipping;

    /** 充血方法 */
    public BigDecimal payableFreight() {
        return freeShipping ? BigDecimal.ZERO : freight;
    }
}
```

### 1.6 错误码与异常规范

| 规则项     | 规范                                                                                                                      |
|------------|---------------------------------------------------------------------------------------------------------------------------|
| 错误码枚举 | `{业务名}ErrorCode implements ErrorCode`，放 `domain/{业务名}/model/enums/`                                               |
| 异常类型   | 统一使用 `common` 的 `BizException`；聚合根、实体、DomainService 都抛它                                                   |
| 抛出方式   | 优先用 `BizAssert.isTrue / notNull / notEmpty`，复杂场景 `throw new BizException(errorCode)`                              |
| 捕获       | domain 层**禁止** try/catch 业务异常；异常直达 `GlobalExceptionHandler`                                                   |
| 通用错误   | 参数为空、资源不存在等可直接用 `CommonErrorCode`，但有业务语义时应定义领域错误码（如 `ORDER_NOT_FOUND` 优于 `NOT_FOUND`） |

```java
/**
 * 订单领域错误码
 */
@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
    ORDER_ITEMS_EMPTY("ORDER_ITEMS_EMPTY", "订单明细不能为空"),
    ORDER_ITEM_NOT_FOUND("ORDER_ITEM_NOT_FOUND", "订单明细不存在"),
    ORDER_ITEM_QUANTITY_INVALID("ORDER_ITEM_QUANTITY_INVALID", "购买数量必须大于 0"),
    ORDER_STATUS_INVALID("ORDER_STATUS_INVALID", "当前订单状态不允许该操作"),
    PAY_AMOUNT_MISMATCH("PAY_AMOUNT_MISMATCH", "支付金额与订单金额不一致"),
    NOT_ORDER_OWNER("NOT_ORDER_OWNER", "无权操作他人订单"),
    RECEIVER_PHONE_BLANK("RECEIVER_PHONE_BLANK", "收货电话不能为空"),
    STOCK_NOT_ENOUGH("STOCK_NOT_ENOUGH", "商品库存不足"),
    ;

    private final String code;
    private final String message;
}
```

### 1.7 Repository 接口定义规范

| 规则项   | 规范                                                          | 示例                                     |
|----------|---------------------------------------------------------------|------------------------------------------|
| 接口名称 | `{聚合根名}Repository`，与 DomainService 的业务名前缀一致     | `OrderDomainService` → `OrderRepository` |
| 定义位置 | `domain/{业务名}/repository/`                                 | —                                        |
| 实现位置 | `infrastructure/{业务名}/repository/{聚合根名}RepositoryImpl` | —                                        |
| 参数     | 聚合根、`{方法名}Query`、基础类型                             | —                                        |
| 禁止     | 出现 PO、`QueryWrapper`、`Page` 等 MyBatis-Flex 类型          | —                                        |

**方法命名与返回值约定**：

| 意图             | 方法名                                 | 返回值                               |
|------------------|----------------------------------------|--------------------------------------|
| 新增或更新聚合根 | `save(aggregate)`                      | `void`（新增时回填 `id`、`version`） |
| 删除聚合根       | `remove(aggregate)` / `removeById(id)` | `void`                               |
| 单条查询         | `find{条件}(...)`                      | `Optional<聚合根>`，禁止返回 null    |
| 列表查询         | `list{条件}(...)`                      | `List<聚合根>`，无数据返回空列表     |
| 分页查询         | `page{条件}(query)`                    | `PageResult<聚合根>`                 |
| 存在性           | `exists{条件}(...)`                    | `boolean`                            |
| 计数             | `count{条件}(...)`                     | `long`                               |

**核心职责**：

- Repository 管理 **领域内**的数据库、缓存操作
- Repository **不依赖 Adaptor**，不调用第三方服务（由 Application 层通过 Adaptor 完成）
- Repository 以聚合根为单位存取，`save` 需要一并处理聚合内的子实体

```java
/**
 * 订单仓储接口
 * 实现在 infrastructure 层 OrderRepositoryImpl
 */
public interface OrderRepository {

    /** 保存聚合根（新增或更新），新增时回填 id */
    void save(OrderAggregate order);

    /** 按 ID 加载聚合根（含订单明细） */
    Optional<OrderAggregate> findById(Long id);

    /** 按订单号加载聚合根 */
    Optional<OrderAggregate> findByOrderNo(String orderNo);

    /** 分页查询买家订单（读模式，不加载明细） */
    PageResult<OrderAggregate> pageByBuyer(PageOrderQuery query);

    /** 后台列表投影：含买家昵称等聚合根之外的字段 */
    PageResult<OrderListItemResult> pageForAdmin(PageOrderQuery query);

    boolean existsByOrderNo(String orderNo);

    /** 逻辑删除聚合根及其子实体 */
    void remove(OrderAggregate order);
}
```

### 1.8 领域枚举规范

| 规则项   | 规范                                                                      |
|----------|---------------------------------------------------------------------------|
| 类名     | `{名词}Enum`（如 `OrderStatusEnum`）                                      |
| 实现     | `implements BaseEnum<Integer>`（或 `String`），`code` 字段加 `@EnumValue` |
| 存放位置 | 单领域使用放 `domain/{业务名}/model/enums/`；跨领域共享放 `common/enums/` |
| 方法     | 允许判断类方法（如 `canCancel()`），禁止修改状态                          |

模板见 `ddd-common-layer.md` 5.2 节。

### 1.9 DomainService 按需查询外部数据的懒加载模式

**场景**：DomainService 在循环中需要按需查询外部数据（如逐个商品查汇率），但 DomainService **禁止依赖 Adaptor**。

**解决方案**：在 Param 中定义一个 **函数式接口属性**，由 Application 层构造 Param 时注入 Adaptor 调用的
lambda。DomainService 只感知接口，不感知 Adaptor。

**适用范围**：仅用于 **循环中按需查询**。非循环场景由 Application 层先通过 Adaptor 取数，再放入 Param。

#### 步骤1：在 domain 层定义函数式接口

```java
package com.florian.sun.spring.template.domain.pricing.model.param;

/**
 * 汇率提供者
 * 定义在 domain 层，由 Application 层注入实现
 */
@FunctionalInterface
public interface ExchangeRateProvider {

    BigDecimal getRate(String currency);
}
```

#### 步骤2：在 Param 中引用该接口

```java

@Data
public class CalculateSalePriceParam {

    private List<ProductItemValue> items;

    /** 懒加载汇率，由 Application 层注入 Adaptor 调用 */
    private ExchangeRateProvider exchangeRateProvider;
}
```

#### 步骤3：DomainService 通过接口按需查询

```java

@Service
public class SalePriceDomainService {

    public CalculateSalePriceResult calculateSalePrice(CalculateSalePriceParam param) {
        List<SalePriceItemValue> items = param.getItems().stream()
                .map(item -> {
                    BigDecimal rate = param.getExchangeRateProvider().getRate(item.currency());
                    return new SalePriceItemValue(item.productId(), item.basePrice().multiply(rate), item.currency());
                })
                .toList();
        return new CalculateSalePriceResult(items);
    }
}
```

#### 步骤4：Application 层注入 Adaptor 实现

```java

@Service
@RequiredArgsConstructor
public class SalePriceCalculateQueryAppService {

    private final ExchangeRateAdaptor exchangeRateAdaptor;
    private final SalePriceDomainService salePriceDomainService;
    private final SalePriceAssembler salePriceAssembler;

    public CalculateSalePriceResponseDTO calculateSalePrice(CalculateSalePriceRequestDTO requestDTO) {
        CalculateSalePriceParam param = salePriceAssembler.toParam(requestDTO);
        // Adaptor 失败会抛 BizException，直接透传给全局处理器
        param.setExchangeRateProvider(currency -> exchangeRateAdaptor.queryExchangeRate(currency).getRate());
        return salePriceAssembler.toResponseDTO(salePriceDomainService.calculateSalePrice(param));
    }
}
```

---

## 二、写模式 domain 规范

### 2.1 DomainService 规范

#### 命名规范

| 规则项   | 规范                                                               | 示例                                           |
|----------|--------------------------------------------------------------------|------------------------------------------------|
| 类名     | `{聚合根名}DomainService`，**直接写具体类**，不再 interface + Impl | `OrderDomainService`                           |
| 注解     | `@Service` + `@RequiredArgsConstructor` 构造注入                   | —                                              |
| 方法命名 | 业务动词，禁止技术动词（`update`、`save`）                         | `createOrder`、`confirmPayment`、`cancelOrder` |
| 参数     | `{方法名}Param`                                                    | `ConfirmPaymentParam`                          |
| 返回值   | `void` 或聚合根                                                    | `OrderAggregate`                               |
| 事务     | **不加** `@Transactional`，事务由 AppService 控制                  | —                                              |

#### 角色定位

- **核心职责**：封装跨聚合根方法的领域流程，维护聚合根的完整性
- **稳定性**：DomainService 方法是稳定的领域能力，被多个 Application 场景复用
- **禁止**：直接访问数据库、调用 Adaptor、读取登录上下文、包含场景编排逻辑

#### 标准流程

1. 通过 Repository 加载聚合根（不存在则抛 `BizException`）
2. 调用聚合根业务方法（校验 + 修改状态）
3. 通过 Repository 持久化

不需要显式加锁：并发冲突由根实体的 `version` 乐观锁在 `save` 时检测，冲突抛 `CommonErrorCode.CONCURRENT_CONFLICT`。

#### 行为约束

- **允许调用**：本领域聚合根方法、本领域 Repository、`common` 静态工具、Hutool 非 IO 工具
- **禁止调用**：AppService、RepositoryImpl、Adaptor、其他领域的 DomainService / 聚合根 / Repository

#### 异常处理

- **不捕获**任何异常，`BizException` 直接向上抛
- 不需要 `log.error`，全局处理器统一记录

#### 代码模板

```java
package com.florian.sun.spring.template.domain.order.service;

/**
 * 订单领域服务（写模式）
 * 稳定的领域能力，被多个 Application 场景复用
 */
@Service
@RequiredArgsConstructor
public class OrderDomainService {

    private final OrderRepository orderRepository;

    /** 创建订单：工厂方法建聚合根 → 持久化 */
    public OrderAggregate createOrder(CreateOrderParam param) {
        OrderAggregate order = OrderAggregate.create(param);
        orderRepository.save(order);
        return order;
    }

    /** 确认支付：加载 → 业务方法 → 持久化 */
    public void confirmPayment(ConfirmPaymentParam param) {
        OrderAggregate order = loadOrder(param.getOrderId());
        order.confirmPayment(param);
        orderRepository.save(order);
    }

    public void cancelOrder(CancelOrderParam param) {
        OrderAggregate order = loadOrder(param.getOrderId());
        order.cancel(param);
        orderRepository.save(order);
    }

    private OrderAggregate loadOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}
```

### 2.2 Aggregate 聚合根规范

#### 核心定义

聚合根是领域模型的核心对象，负责维护一组相关实体和值对象的业务一致性边界。外部只能通过聚合根的业务方法修改内部状态。

#### 设计原则

1. **单一职责**：一个聚合根只负责一个核心业务概念
2. **强一致性**：聚合内的所有修改在一次 `save` 中落库，同一事务
3. **小聚合**：避免把关联对象全部塞进一个聚合；跨聚合通过 ID 引用
4. **封装性**：状态只能通过业务方法修改
5. **聚合根是容器**：聚合根只持有实体与值对象，属性与单实体规则下沉到实体

#### 字段约束：聚合根只持有实体与值对象

| 规则项       | 规范                                                                                                                                   |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------|
| 根实体       | **必有且仅有一个** `{名词}Entity` 字段，与主表一一对应，字段名用业务名词（`order`、`user`）；`rootEntity()` 返回它                     |
| 子实体       | `{名词}Entity` 或 `List<{名词}Entity>`，与子表一一对应                                                                                 |
| 值对象       | `{名词}Value`（record）；通常挂在所属实体上，跨实体的聚合级概念可直接挂在聚合根上                                                      |
| 禁止的字段   | `String`、`Long`、枚举、`BigDecimal`、`LocalDateTime` 等基础类型 / 简单类型字段；`id`、`version` 等元数据（它们在 `BaseEntity` 上）    |
| 禁止透传     | 不写 `getBuyerId() { return order.getBuyerId(); }` 这类透传 getter；读取属性统一走 `aggregate.getOrder().getBuyerId()`                 |
| 单表聚合     | 只有根实体、没有子实体的聚合（如用户、文件）是正常形态：聚合根就是「薄壳 + 根实体」，方法全部委托根实体，不要为了「显得有内容」把属性拉回聚合根 |

**职责划分**：

| 位置   | 负责                                                                                                          |
|--------|---------------------------------------------------------------------------------------------------------------|
| 聚合根 | 对外唯一入口；工厂方法组装实体；**跨实体**的一致性规则与计算（如按明细算总额）；单实体规则直接委托给实体方法 |
| 实体   | 持有属性；**只修改自身字段、校验自身不变量**；不引用其他实体（跨实体逻辑由聚合根做）                          |
| 值对象 | 不可变；紧凑构造器校验不变量；只提供计算 / 判断方法                                                           |

#### 命名规范

| 规则项   | 规范                                                                                         | 示例                           |
|----------|----------------------------------------------------------------------------------------------|--------------------------------|
| 类名     | `{名词}Aggregate extends BaseAggregate`                                                      | `OrderAggregate`               |
| Lombok   | `@Getter @Setter`，禁止 `@Data`（会生成基于所有字段的 equals，聚合根应按 id 比较）           | —                              |
| 构造     | 新建走静态工厂 `create({方法名}Param)`，内部调用各实体的 `create`；保留无参构造供 Converter 重建 | `OrderAggregate.create(param)` |
| 根实体   | 覆写 `protected BaseEntity rootEntity()` 返回根实体字段                                       | `return order;`                |
| 方法命名 | 动词                                                                                         | `confirmPayment`、`cancel`     |
| 参数     | `{方法名}Param` 或基础类型                                                                   | —                              |
| 返回值   | `void`、基础类型、值对象、实体                                                               | —                              |

#### setter 使用规则

- setter 存在的 **唯一目的**是让 infrastructure 的 MapStruct Converter 从 PO 重建聚合根 / 实体，以及让 RepositoryImpl 回填 `id` / `version`
- `domain`、`application` 代码 **禁止**调用聚合根 / 实体的 setter 修改状态，必须通过业务方法
- 实体内部修改自身字段直接用 `this.field = ...`；聚合根内部只在工厂方法里给实体字段赋值

#### 四种方法类型

**1. 写操作类方法**（修改状态）：聚合根方法是入口，单实体规则委托给实体，实体内部做校验 + 修改状态

```java
// 聚合根：入口 + 委托
public void confirmPayment(ConfirmPaymentParam param) {
    order.confirmPayment(param);
}

// 根实体：校验自身不变量并修改自身字段
public void confirmPayment(ConfirmPaymentParam param) {
    BizAssert.isTrue(this.status == OrderStatusEnum.WAIT_PAY, OrderErrorCode.ORDER_STATUS_INVALID);
    BizAssert.isTrue(this.totalAmount.compareTo(param.getPaidAmount()) == 0, OrderErrorCode.PAY_AMOUNT_MISMATCH);
    this.status = OrderStatusEnum.PAID;
    this.payTime = param.getPayTime();
}
```

**2. 计算类方法**（不修改状态）：跨实体计算放聚合根，单实体计算放实体

```java
// 聚合根：跨实体（遍历子实体）
public BigDecimal calculateTotalAmount() {
    return items.stream()
            .map(OrderItemEntity::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

**3. 查找类方法**（不修改状态）：

```java
public OrderItemEntity findItem(Long itemId) {
    return items.stream()
            .filter(item -> Objects.equals(item.getId(), itemId))
            .findFirst()
            .orElseThrow(() -> new BizException(OrderErrorCode.ORDER_ITEM_NOT_FOUND));
}
```

**4. 判断类方法**（不修改状态）：

```java
// 聚合根：委托
public boolean isPaid() {
    return order.isPaid();
}

// 根实体
public boolean isPaid() {
    return this.status == OrderStatusEnum.PAID;
}
```

> 聚合根不会直接被序列化成 HTTP 响应（输出走 ResponseDTO），因此不需要 `@JsonIgnore` 之类的注解。但 MapStruct 在 Aggregate →
> DTO 时会把 `isXxx()` / `getXxx()` 识别为属性，若 DTO 恰好有同名字段会被自动映射；不希望映射时在 Assembler 中
> `@Mapping(target = "xxx", ignore = true)`。根实体的属性通过 `@Mapping(target = ".", source = "order")` 平铺到 DTO，见
> `ddd-application-layer.md` 1.9 节。

#### 方法膨胀控制策略

- **状态流转方法**：严格对应业务状态变化（`confirmPayment`、`cancel`、`ship`），不加技术性的 `updateStatus`
- **补充数据类方法**：按业务语义合并（如 `fillReceiver` 代替多个字段的 `setXxx`），方法名体现业务意图
- **读方法**：数量多时可让调用方通过 `findItem` 拿到实体后调用实体自己的方法，减少聚合根方法数
- **委托方法**：聚合根对外暴露的方法与根实体一一对应是正常的；不要因为「只是转发」就把规则挪回聚合根

#### 代码模板

```java
package com.florian.sun.spring.template.domain.order.model.aggregate;

/**
 * 订单聚合根
 * 聚合边界：订单根实体 + 订单明细子实体；聚合根本身不持有基础类型字段
 */
@Getter
@Setter
public class OrderAggregate extends BaseAggregate {

    /** 根实体：t_order */
    private OrderEntity order;

    /** 子实体：t_order_item */
    private List<OrderItemEntity> items = new ArrayList<>();

    @Override
    protected BaseEntity rootEntity() {
        return order;
    }

    /** 工厂方法：新建订单的唯一入口，组装实体并维护跨实体一致性 */
    public static OrderAggregate create(CreateOrderParam param) {
        BizAssert.notEmpty(param.getItems(), OrderErrorCode.ORDER_ITEMS_EMPTY);
        OrderAggregate aggregate = new OrderAggregate();
        aggregate.items = param.getItems().stream().map(OrderItemEntity::create).collect(Collectors.toList());
        aggregate.order = OrderEntity.create(param, aggregate.calculateTotalAmount());
        return aggregate;
    }

    /** 单实体规则：委托根实体 */
    public void confirmPayment(ConfirmPaymentParam param) {
        order.confirmPayment(param);
    }

    public void cancel(CancelOrderParam param) {
        order.cancel(param);
    }

    /** 跨实体计算：聚合根负责 */
    public BigDecimal calculateTotalAmount() {
        return items.stream().map(OrderItemEntity::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public OrderItemEntity findItem(Long itemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new BizException(OrderErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    public boolean isPaid() {
        return order.isPaid();
    }

    public boolean isOwnedBy(Long userId) {
        return order.isOwnedBy(userId);
    }
}
```

### 2.3 Entity 实体规范

实体分两类：**根实体**（每个聚合恰好一个，与主表对应，聚合的 `id` / `version` 来自它）与 **子实体**（与子表对应，通过 `{父表}Id` 关联）。两者规范相同：

| 规则项   | 规范                                                                                  | 示例                              |
|----------|---------------------------------------------------------------------------------------|-----------------------------------|
| 类名     | `{名词}Entity extends BaseEntity`                                                     | `OrderEntity`、`OrderItemEntity`  |
| Lombok   | `@Getter @Setter`（`BaseEntity` 已按 id 实现 equals）                                 | —                                 |
| 归属     | 只能通过聚合根访问，不能被 Repository 单独存取                                        | —                                 |
| 属性类型 | 普通字段、领域枚举、值对象（不再使用 `Field<T>`）；`id`/`version`/时间戳继承自基类    | —                                 |
| 构造     | 静态工厂 `create(...)` 校验自身不变量；保留无参构造供 Converter 重建                  | `OrderEntity.create(param, total)` |
| 方法     | 只修改 **自身**字段、校验 **自身**不变量；不持有、不引用其他实体                       | `confirmPayment`、`subtotal`      |
| 异常     | `BizException`                                                                        | —                                 |

#### 根实体模板

```java
package com.florian.sun.spring.template.domain.order.model.entity;

import cn.hutool.core.util.IdUtil;

/**
 * 订单根实体
 * 与 t_order 一一对应，持有订单自身属性与单实体规则
 */
@Getter
@Setter
public class OrderEntity extends BaseEntity {

    private String orderNo;
    private Long buyerId;
    private OrderStatusEnum status;
    private BigDecimal totalAmount;
    private AddressValue receiver;
    private LocalDateTime payTime;
    private String cancelReason;

    /** 总额由聚合根按明细算好后传入，实体不感知子实体 */
    public static OrderEntity create(CreateOrderParam param, BigDecimal totalAmount) {
        OrderEntity order = new OrderEntity();
        order.orderNo = "O" + IdUtil.getSnowflakeNextIdStr();
        order.buyerId = param.getBuyerId();
        order.receiver = param.getReceiver();
        order.status = OrderStatusEnum.WAIT_PAY;
        order.totalAmount = totalAmount;
        return order;
    }

    public void confirmPayment(ConfirmPaymentParam param) {
        BizAssert.isTrue(status == OrderStatusEnum.WAIT_PAY, OrderErrorCode.ORDER_STATUS_INVALID);
        BizAssert.isTrue(totalAmount.compareTo(param.getPaidAmount()) == 0, OrderErrorCode.PAY_AMOUNT_MISMATCH);
        this.status = OrderStatusEnum.PAID;
        this.payTime = param.getPayTime();
    }

    public void cancel(CancelOrderParam param) {
        BizAssert.isTrue(Objects.equals(buyerId, param.getOperatorId()), OrderErrorCode.NOT_ORDER_OWNER);
        BizAssert.isTrue(status.canCancel(), OrderErrorCode.ORDER_STATUS_INVALID);
        this.status = OrderStatusEnum.CANCELLED;
        this.cancelReason = param.getReason();
    }

    public boolean isPaid() {
        return status == OrderStatusEnum.PAID;
    }

    public boolean isOwnedBy(Long userId) {
        return Objects.equals(buyerId, userId);
    }
}
```

#### 子实体模板

```java
package com.florian.sun.spring.template.domain.order.model.entity;

/**
 * 订单明细子实体
 * 与 t_order_item 一一对应
 */
@Getter
@Setter
public class OrderItemEntity extends BaseEntity {

    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;

    public static OrderItemEntity create(OrderItemValue value) {
        BizAssert.isTrue(value.quantity() > 0, OrderErrorCode.ORDER_ITEM_QUANTITY_INVALID);
        OrderItemEntity entity = new OrderItemEntity();
        entity.productId = value.productId();
        entity.productName = value.productName();
        entity.price = value.price();
        entity.quantity = value.quantity();
        return entity;
    }

    public BigDecimal subtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
```

### 2.4 Value 值对象规范

#### 核心特征

| 特性   | Value Object  | Entity                     |
|--------|---------------|----------------------------|
| 标识   | 无            | 有唯一 ID                  |
| 相等性 | 属性值相等    | ID 相等                    |
| 可变性 | **不可变**    | 可变（写模式）             |
| 实现   | Java `record` | `class extends BaseEntity` |

#### 命名规范

| 规则项 | 规范                         | 示例                               |
|--------|------------------------------|------------------------------------|
| 类名   | `{名词}Value`，使用 `record` | `AddressValue`、`OrderItemValue`   |
| 方法   | 只能有计算类、判断类方法；允许静态工厂封装构造规则 | `fullAddress()`、`PasswordValue.encode(...)` |
| 校验   | 在紧凑构造器中校验不变量     | —                                  |
| 归属   | 作为实体字段（落库展开为该实体所在表的列）；跨实体的聚合级概念可直接作为聚合根字段 | `OrderEntity.receiver`             |

```java
package com.florian.sun.spring.template.domain.order.model.value;

/**
 * 收货地址值对象
 */
public record AddressValue(String province, String city, String detail, String receiverName, String receiverPhone) {

    public AddressValue {
        BizAssert.notBlank(receiverPhone, OrderErrorCode.RECEIVER_PHONE_BLANK);
    }

    public String fullAddress() {
        return province + city + detail;
    }
}
```

> 值对象落库时通常展开为所属实体对应表的多个列（`receiver_province`、`receiver_city` …），由 Converter 负责拆装；
> 单字段值对象（如 `PasswordValue` ↔ `password_hash`）在 Converter 里用 `default` 方法互转。

### 2.5 Repository 写模式规范

| 规则项      | 规范                                                                                         |
|-------------|----------------------------------------------------------------------------------------------|
| 方法        | `save`、`findById`、`findBy{业务键}`、`remove`                                               |
| `save` 语义 | `id == null` 新增，否则按 `version` 乐观锁更新；一并处理子实体；新增后回填 `id` 与 `version` |
| 返回值      | `save` 返回 `void`；`find*` 返回 `Optional`                                                  |

```java
public interface OrderRepository {

    void save(OrderAggregate order);

    Optional<OrderAggregate> findById(Long id);

    Optional<OrderAggregate> findByOrderNo(String orderNo);

    void remove(OrderAggregate order);
}
```

### 2.6 用户认证示例（单表聚合 + 值对象 + domain 直接使用 Hutool）

用户是典型的 **单表聚合**：`UserAggregate` 只有一个根实体 `UserEntity`，方法全部委托；密码封装为值对象 `PasswordValue`，
内部直接调用 Hutool 的 `DigestUtil.bcrypt()` / `bcryptCheck()`，不额外封装密码工具类。新密码先校验非空且 UTF-8 编码后不超过 72 字节。

```java
package com.florian.sun.spring.template.domain.user.model.aggregate;

/**
 * 用户聚合根
 * 单表聚合：只有根实体，方法全部委托
 */
@Getter
@Setter
public class UserAggregate extends BaseAggregate {

    /** 根实体：t_user */
    private UserEntity user;

    @Override
    protected BaseEntity rootEntity() {
        return user;
    }

    public static UserAggregate create(RegisterUserParam param) {
        UserAggregate aggregate = new UserAggregate();
        aggregate.user = UserEntity.create(param);
        return aggregate;
    }

    public void verifyPassword(String rawPassword) {
        user.verifyPassword(rawPassword);
    }

    public void ensureActive() {
        user.ensureActive();
    }
}
```

```java
package com.florian.sun.spring.template.domain.user.model.entity;

/**
 * 用户根实体
 * 与 t_user 一一对应
 */
@Getter
@Setter
public class UserEntity extends BaseEntity {

    private String email;
    private PasswordValue password;
    private String nickname;
    private Long avatarFileId;
    private UserRoleEnum role;
    private UserStatusEnum status;

    public static UserEntity create(RegisterUserParam param) {
        UserEntity user = new UserEntity();
        user.email = param.getEmail();
        user.password = PasswordValue.encode(param.getRawPassword());
        user.nickname = param.getNickname();
        user.role = UserRoleEnum.USER;
        user.status = UserStatusEnum.ACTIVE;
        return user;
    }

    public void verifyPassword(String rawPassword) {
        BizAssert.isTrue(password.matches(rawPassword), UserErrorCode.EMAIL_OR_PASSWORD_INCORRECT);
    }

    public void ensureActive() {
        BizAssert.isTrue(status == UserStatusEnum.ACTIVE, UserErrorCode.USER_DISABLED);
    }
}
```

```java
package com.florian.sun.spring.template.domain.user.model.value;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;

/**
 * 密码值对象
 * 只持有 BCrypt 哈希；落库为 t_user.password_hash 单列
 */
public record PasswordValue(String hash) {

    /** BCrypt 只取前 72 字节，超长部分会被静默截断，这里显式拒绝 */
    private static final int MAX_RAW_BYTES = 72;

    public PasswordValue {
        BizAssert.notBlank(hash, UserErrorCode.PASSWORD_INVALID);
    }

    /** 明文 → 哈希，创建用户 / 修改密码时使用 */
    public static PasswordValue encode(String rawPassword) {
        BizAssert.notBlank(rawPassword, UserErrorCode.PASSWORD_INVALID);
        BizAssert.isTrue(rawPassword.getBytes(StandardCharsets.UTF_8).length <= MAX_RAW_BYTES, UserErrorCode.PASSWORD_INVALID);
        return new PasswordValue(DigestUtil.bcrypt(rawPassword));
    }

    public boolean matches(String rawPassword) {
        return StrUtil.isNotBlank(rawPassword) && DigestUtil.bcryptCheck(rawPassword, hash);
    }
}
```

```java
package com.florian.sun.spring.template.domain.user.service;

/**
 * 用户领域服务
 */
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final UserRepository userRepository;

    /** 认证：邮箱 + 密码 + 账号状态，全部通过返回聚合根，否则抛 BizException */
    public UserAggregate authenticate(AuthenticateParam param) {
        UserAggregate user = userRepository.findByEmail(param.getEmail())
                .orElseThrow(() -> new BizException(UserErrorCode.EMAIL_OR_PASSWORD_INCORRECT));
        user.verifyPassword(param.getRawPassword());
        user.ensureActive();
        return user;
    }
}
```

> 用户不存在与密码错误返回同一个错误码，避免泄漏账号是否存在。登录态（token）的签发不属于领域，由 Application 通过
> `SessionAdaptor` 完成，见 `ddd-application-layer.md` 2.4 节。

---

## 三、读模式 domain 规范

读模式是 **纯查询**，不包含业务逻辑。聚合根作为 **数据载体**，不调用其业务方法。

**特点**：

- 无 DomainService（读模式不经过领域服务，QueryAppService 直接调用 Repository）
- 复用同一个聚合根类作数据载体；列表查询可以不加载子实体
- 当查询结果与聚合根结构差异大（如需要 join 出买家昵称）时，Repository 返回 `{方法名}Result`（定义在 `domain/model/result/`
  ），而不是给聚合根加无关字段

### 3.1 Repository 读模式规范

| 规则项   | 规范                                                                                   | 示例                      |
|----------|----------------------------------------------------------------------------------------|---------------------------|
| 方法命名 | `find` / `list` / `page` / `exists` / `count` 前缀，体现查询意图                       | `findById`、`pageByBuyer` |
| 参数     | `{方法名}Query`（分页继承 `PageQuery`）或基础类型                                      | `PageOrderQuery`、`Long`  |
| 返回值   | `Optional<聚合根>`、`List<聚合根>`、`PageResult<聚合根>`、`PageResult<{方法名}Result>` | —                         |

```java
public interface OrderRepository {

    Optional<OrderAggregate> findById(Long id);

    /** 分页列表，不加载明细 */
    PageResult<OrderAggregate> pageByBuyer(PageOrderQuery query);

    /** 后台列表投影：含买家昵称等聚合根之外的字段 */
    PageResult<OrderListItemResult> pageForAdmin(PageOrderQuery query);
}
```

---

## 四、纯计算模式 domain 规范

### 4.1 DomainService 规范

#### 定义与核心特征

- **无聚合根和实体**：业务逻辑完全由 DomainService 承载
- **无状态**：不修改任何状态，仅基于输入参数计算，方法幂等
- **无 Repository 依赖**：所需数据全部由 Param 传入（或通过 1.9 节的懒加载接口获取）

**典型场景**：运费试算、积分预估、报表汇总

#### 命名规范

| 规则项   | 规范                  | 示例                            |
|----------|-----------------------|---------------------------------|
| 类名     | `{动词}DomainService` | `FreightCalculateDomainService` |
| 方法命名 | 动词                  | `calculateFreight`              |
| 参数     | `{方法名}Param`       | `CalculateFreightParam`         |
| 返回值   | `{方法名}Result`      | `CalculateFreightResult`        |

#### 代码模板

```java
package com.florian.sun.spring.template.domain.freight.service;

/**
 * 运费计算领域服务（纯计算模式）
 * 无状态，无 Repository
 */
@Service
public class FreightCalculateDomainService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("99");
    private static final BigDecimal BASE_FREIGHT = new BigDecimal("10");

    public CalculateFreightResult calculateFreight(CalculateFreightParam param) {
        CalculateFreightResult result = new CalculateFreightResult();
        if (param.getGoodsAmount().compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            result.setFreeShipping(true);
            result.setFreight(BigDecimal.ZERO);
            return result;
        }
        BigDecimal freight = param.isRemoteArea() ? BASE_FREIGHT.multiply(new BigDecimal("2")) : BASE_FREIGHT;
        result.setFreeShipping(false);
        result.setFreight(freight);
        return result;
    }
}
```

### 4.2 Result 规范

同 1.5 节，`{方法名}Result`，可以有充血方法。

---

## 五、规则+计算模式 domain 规范

### 5.1 DomainService 规范

#### 定义与核心特征

- **规则建模为聚合根**：如 `CouponRuleAggregate`
- **业务逻辑由聚合根承载**：DomainService 只负责加载规则、遍历、汇总， **不包含**匹配和计算逻辑
- **无副作用**：聚合根方法只读自身规则数据并返回结果，不修改状态

#### 命名规范

| 规则项   | 规范                  | 示例                             |
|----------|-----------------------|----------------------------------|
| 类名     | `{动词}DomainService` | `DiscountCalculateDomainService` |
| 方法命名 | 动词                  | `calculateDiscount`              |
| 参数     | `{方法名}Param`       | `CalculateDiscountParam`         |
| 返回值   | `{方法名}Result`      | `CalculateDiscountResult`        |

#### 方法实现流程

1. 通过 Repository 加载规则聚合根集合
2. 按优先级排序
3. 逐条调用聚合根的 `match` / `calculate` 方法
4. 汇总为 Result

#### 代码模板

```java
package com.florian.sun.spring.template.domain.coupon.service;

/**
 * 优惠计算领域服务（规则+计算模式）
 * 只做编排，匹配与计算逻辑在 CouponRuleAggregate 中
 */
@Service
@RequiredArgsConstructor
public class DiscountCalculateDomainService {

    private final CouponRuleRepository couponRuleRepository;

    public CalculateDiscountResult calculateDiscount(CalculateDiscountParam param) {
        List<CouponRuleAggregate> rules = couponRuleRepository.listEnabled();
        BizAssert.notEmpty(rules, CouponErrorCode.RULE_EMPTY);

        rules.sort(Comparator.comparingInt((CouponRuleAggregate r) -> r.getRule().getPriority()).reversed());

        for (CouponRuleAggregate rule : rules) {
            if (rule.match(param)) {
                return new CalculateDiscountResult(rule.getId(), rule.calculateDiscount(param));
            }
        }
        return CalculateDiscountResult.noDiscount();
    }
}
```

### 5.2 规则 Aggregate 规范

- 规则聚合根提供 **无副作用**的 `match` / `calculate` 方法，委托给根实体
- 匹配与计算逻辑必须内聚在实体中， **禁止泄漏到 DomainService**
- 每个方法聚焦单一职责，方法名表达业务意图

```java
package com.florian.sun.spring.template.domain.coupon.model.aggregate;

/**
 * 优惠规则聚合根
 * 单表聚合：只有根实体
 */
@Getter
@Setter
public class CouponRuleAggregate extends BaseAggregate {

    /** 根实体：t_coupon_rule */
    private CouponRuleEntity rule;

    @Override
    protected BaseEntity rootEntity() {
        return rule;
    }

    public boolean match(CalculateDiscountParam param) {
        return rule.match(param);
    }

    public BigDecimal calculateDiscount(CalculateDiscountParam param) {
        return rule.calculateDiscount(param);
    }
}
```

```java
package com.florian.sun.spring.template.domain.coupon.model.entity;

/**
 * 优惠规则根实体
 */
@Getter
@Setter
public class CouponRuleEntity extends BaseEntity {

    private String ruleName;
    private Integer priority;
    private BigDecimal thresholdAmount;
    private DiscountTypeEnum discountType;
    private BigDecimal discountValue;
    private Boolean enabled;

    /** 匹配规则（无副作用） */
    public boolean match(CalculateDiscountParam param) {
        return Boolean.TRUE.equals(enabled)
                && param.getOrderAmount().compareTo(thresholdAmount) >= 0;
    }

    /** 计算优惠金额（无副作用） */
    public BigDecimal calculateDiscount(CalculateDiscountParam param) {
        return switch (discountType) {
            case FIXED -> discountValue.min(param.getOrderAmount());
            case PERCENT -> param.getOrderAmount().multiply(discountValue).setScale(2, RoundingMode.HALF_UP);
        };
    }
}
```
