---
description: infrastructure 基础设施层开发规范：RepositoryImpl、MyBatis-Flex PO/Mapper/QueryWrapper、MapStruct Converter、MySQL 表约定、Redis 缓存、Sa-Token StpInterfaceImpl，四种开发模式
alwaysApply: true
---

# infrastructure 层开发规范

## 一、基础规范（所有模式通用）

### 1.1 包结构规范

infrastructure 层按业务领域划分顶层包，领域下的子包结构保持不变：

```plain
infrastructure/
├── auth/                          # Sa-Token 权限数据源 StpInterfaceImpl
└── {业务名}/                      # 按业务领域划分（如：order、user）
    ├── repository/                # {聚合根名}RepositoryImpl
    ├── mysql/
    │   ├── po/                    # {表对应业务名}PO extends BasePO
    │   │   └── table/             # APT 自动生成的 {表对应业务名}TableDef（不要手写）
    │   └── mapper/                # {表对应业务名}Mapper extends BaseMapper<PO>
    ├── converter/                 # MapStruct Converter：PO ↔ 聚合根 / 实体
    └── cache/                     # Redis 缓存实现（可选）
```

`BasePO` 放在 `infrastructure/common/po/`，与业务包并列。

### 1.2 核心定位

**Infrastructure 层是技术实现层，负责把 domain 层定义的 Repository 接口落地为 MyBatis-Flex / Redis 的具体实现。**

- 实现 domain 层的 Repository 接口，对 domain 屏蔽所有技术细节
- 仅做**纯技术转换**（实体 ↔ PO，再把实体装进聚合根），**禁止包含任何业务逻辑**
- 业务逻辑属于 domain 层，Infrastructure 只负责"存"和"取"

#### 与其他层的关系

| 关系                  | 说明                                                                        |
|-----------------------|-----------------------------------------------------------------------------|
| **与 domain 层**      | 实现 Repository 接口；可以使用聚合根、实体、值对象、Query、Result、领域枚举 |
| **与 application 层** | 无直接依赖；Application 通过 Repository 接口间接调用                        |
| **与 adaptor 层**     | 无依赖                                                                      |
| **与 common 层**      | 可依赖 `PageResult`、`BizException`、`CommonErrorCode`                      |

### 1.3 RepositoryImpl 命名规范

| 规则项       | 规范                                                                    | 示例                  |
|--------------|-------------------------------------------------------------------------|-----------------------|
| 类名         | `{聚合根名}RepositoryImpl`                                              | `OrderRepositoryImpl` |
| 命名对应关系 | 与 domain 的 `{聚合根名}Repository`、`{聚合根名}DomainService` 前缀一致 | —                     |
| 注解         | `@Repository` + `@RequiredArgsConstructor`                              | —                     |
| 实现接口     | `implements {聚合根名}Repository`                                       | —                     |
| 事务         | **不加** `@Transactional`，事务由 AppService 控制                       | —                     |

### 1.4 允许与禁止规范

| 维度         | 允许                                                                                                     | 禁止                                                                              |
|--------------|----------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| **模块依赖** | `domain`、`common`                                                                                       | `application`、`adaptor`                                                          |
| **技术使用** | MyBatis-Flex（`BaseMapper`、`QueryWrapper`、`TableDef`、`Page`）、`StringRedisTemplate`、Spring 事件发布 | `RestClient` / HTTP 调用（属于 adaptor）、`StpUtil`（`infrastructure/auth` 除外） |
| **代码职责** | 实现 Repository、Converter 纯字段映射、Mapper 查询、缓存读写、乐观锁冲突翻译                             | 业务判断、在 Converter 中写 `if` 业务逻辑、向 domain 暴露 PO / `Page`             |
| **调用方式** | 通过 Repository 接口被调用                                                                               | 被 Application 以实现类方式注入；Mapper 被 RepositoryImpl 之外的类调用            |

### 1.5 异常处理规范

| 情况                                  | 处理                                                                   |
|---------------------------------------|------------------------------------------------------------------------|
| 乐观锁更新影响行数为 0                | 抛 `BizException(CommonErrorCode.CONCURRENT_CONFLICT)`                 |
| 唯一键冲突（`DuplicateKeyException`） | 有业务语义时翻译为领域错误码（如 `ORDER_NO_DUPLICATED`），否则放行     |
| 其他 `DataAccessException`            | **不捕获**，由 `GlobalExceptionHandler` 兜底为 `SYSTEM_ERROR` 并打堆栈 |
| 查询不到                              | 返回 `Optional.empty()` / 空列表 / 空 `PageResult`，**不抛异常**       |

```java
// ✅ 正确：乐观锁冲突翻译为业务异常，其余异常放行
int rows = orderMapper.update(po);
BizAssert.isTrue(rows == 1, CommonErrorCode.CONCURRENT_CONFLICT);

// ❌ 错误：吞掉所有异常，事务不会回滚，上层拿到不完整数据
try {
    orderMapper.update(po);
} catch (Exception e) {
    log.error("更新失败", e);
}
```

### 1.6 MySQL 表约定

| 项目     | 约定                                                                                    |
|----------|-----------------------------------------------------------------------------------------|
| 表名     | `t_{业务名}`，snake_case，如 `t_order`、`t_order_item`                                  |
| 字符集   | `utf8mb4`，引擎 InnoDB                                                                  |
| 主键     | `id BIGINT AUTO_INCREMENT`                                                              |
| 审计字段 | `create_time DATETIME`、`update_time DATETIME`，每张表必有                              |
| 逻辑删除 | `deleted TINYINT NOT NULL DEFAULT 0`，每张表必有                                        |
| 乐观锁   | `version INT NOT NULL DEFAULT 0`，每张表必有（子表也带，保证所有 PO 都能复用 `BasePO`） |
| 枚举     | 存 `TINYINT` / `INT`，注释写清枚举含义                                                  |
| 金额     | `DECIMAL(12,2)`                                                                         |
| 值对象   | 展开为多个列，列名加前缀（`receiver_province`、`receiver_city`）                        |
| 索引命名 | 唯一索引 `uk_{列}`，普通索引 `idx_{列}`                                                 |
| 外键     | 不建物理外键，用 `xxx_id` 列 + 索引表达关联                                             |

```sql
CREATE TABLE t_order (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no          VARCHAR(32)  NOT NULL COMMENT '订单号',
    buyer_id          BIGINT       NOT NULL COMMENT '买家ID',
    status            TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待支付 1已支付 2已取消',
    total_amount      DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    receiver_province VARCHAR(32)  NOT NULL COMMENT '收货省',
    receiver_city     VARCHAR(32)  NOT NULL COMMENT '收货市',
    receiver_detail   VARCHAR(255) NOT NULL COMMENT '收货详细地址',
    receiver_name     VARCHAR(64)  NOT NULL COMMENT '收货人',
    receiver_phone    VARCHAR(20)  NOT NULL COMMENT '收货电话',
    pay_time          DATETIME     NULL COMMENT '支付时间',
    cancel_reason     VARCHAR(255) NULL COMMENT '取消原因',
    version           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_buyer_id (buyer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单';
```

### 1.7 PO（Persistent Object）规范

PO 与数据库表一一对应，仅在 Infrastructure 层内部流转，**禁止**暴露给 domain / application 层。

#### 命名规范

| 规则项   | 规范                                                               | 示例                     |
|----------|--------------------------------------------------------------------|--------------------------|
| 类名     | `{表对应业务名}PO extends BasePO`                                  | `OrderPO`、`OrderItemPO` |
| 存放位置 | `infrastructure/{业务名}/mysql/po/`                                | —                        |
| 字段命名 | 驼峰，靠 `map-underscore-to-camel-case` 映射到 snake_case 列       | `orderNo` ↔ `order_no`   |
| 注解     | `@Table("t_xxx")`、`@Data`、`@EqualsAndHashCode(callSuper = true)` | —                        |

#### 设计原则

- PO 是**纯数据载体**，只有字段和 getter/setter，**禁止**业务方法
- 枚举字段直接使用领域枚举类型（枚举上有 `@EnumValue`），MyBatis-Flex 自动按 `code` 落库
- 值对象在 PO 中展开为多个平铺字段
- 子表 PO 通过 `{父表}Id` 字段关联父表

#### BasePO 模板

```java
package com.florian.sun.spring.template.infrastructure.common.po;

/**
 * PO 基类
 * 统一主键、审计字段、逻辑删除、乐观锁
 */
@Data
public abstract class BasePO {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 乐观锁，MyBatis-Flex 更新时自动带 version 条件并 +1 */
    @Column(version = true)
    private Integer version;

    /** 逻辑删除：0 正常 1 删除，查询自动追加 deleted = 0 */
    @Column(isLogicDelete = true)
    private Integer deleted;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
```

> MyBatis-Flex APT 会处理父类字段，`BasePO` 中的列同样会出现在生成的 `TableDef` 中。所有表都带 `version`、`deleted`、`create_time`、`update_time` 四列，所有 PO 都继承 `BasePO`，不做特例。

#### PO 模板

```java
package com.florian.sun.spring.template.infrastructure.order.mysql.po;

/**
 * 订单持久化对象
 * 与 t_order 表一一对应
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_order")
public class OrderPO extends BasePO {

    private String orderNo;
    private Long buyerId;

    /** 领域枚举，靠 @EnumValue 自动按 code 落库 */
    private OrderStatusEnum status;

    private BigDecimal totalAmount;

    /** 值对象 AddressValue 展开 */
    private String receiverProvince;
    private String receiverCity;
    private String receiverDetail;
    private String receiverName;
    private String receiverPhone;

    private LocalDateTime payTime;
    private String cancelReason;
}
```

#### APT 配置

在项目根目录（`pom.xml` 同级）新建 `mybatis-flex.config`，让生成的 `TableDef` 去掉 `PO` 后缀：

```properties
processor.tableDef.ignoreEntitySuffixes=PO
```

生成结果：`infrastructure/order/mysql/po/table/OrderTableDef`，静态常量 `OrderTableDef.ORDER`，列常量 `ORDER.ORDER_NO`、`ORDER.BUYER_ID`。RepositoryImpl 中 `import static ...table.OrderTableDef.ORDER;`。

### 1.8 Mapper 接口规范

Mapper 是 MyBatis-Flex 数据访问接口。

| 规则项        | 规范                                                                                                               | 示例          |
|---------------|--------------------------------------------------------------------------------------------------------------------|---------------|
| 接口名        | `{表对应业务名}Mapper extends BaseMapper<{表对应业务名}PO>`                                                        | `OrderMapper` |
| 存放位置      | `infrastructure/{业务名}/mysql/mapper/`                                                                            | —             |
| 扫描方式      | `config/MyBatisFlexConfig` 的 `@MapperScan(markerInterface = BaseMapper.class)` 统一扫描，**不要**逐个加 `@Mapper` | —             |
| 自定义方法    | 优先用 `QueryWrapper` 在 RepositoryImpl 中拼条件；复杂 SQL 用 `@Select` 注解方法或 `default` 方法                  | —             |
| 参数 / 返回值 | PO、基础类型、`QueryWrapper`；**禁止**出现聚合根、DTO                                                              | —             |

**设计原则**：

- Mapper 仅被同业务域的 RepositoryImpl 调用（`infrastructure/auth` 例外，见 1.12 节），**禁止**被 domain / application / adaptor 调用
- `QueryWrapper`、`TableDef`、`Page` 只允许出现在 RepositoryImpl 与 Mapper 内部
- `QueryWrapper` 对 `null` 参数**自动忽略条件**，动态条件不需要手写 `if`

```java
package com.florian.sun.spring.template.infrastructure.order.mysql.mapper;

/**
 * 订单 Mapper
 * BaseMapper 已提供 insert / update / delete / select / paginate，通常无需自定义方法
 */
public interface OrderMapper extends BaseMapper<OrderPO> {

    /** 复杂统计类 SQL 示例 */
    @Select("SELECT COUNT(*) FROM t_order WHERE buyer_id = #{buyerId} AND status = 1 AND deleted = 0")
    long countPaidByBuyer(@Param("buyerId") Long buyerId);
}
```

**常用 `BaseMapper` 方法对照**：

| 意图                                          | 方法                                                                                |
|-----------------------------------------------|-------------------------------------------------------------------------------------|
| 新增（跳过 null 字段，配合数据库默认值）      | `insertSelective(po)`，主键回填到 `po.getId()`                                      |
| 批量新增                                      | `insertBatch(list)`                                                                 |
| 按主键更新（忽略 null 字段，带 version 条件） | `update(po)`，返回影响行数                                                          |
| 逻辑删除                                      | `deleteById(id)`、`deleteByQuery(wrapper)`（自动变为 `UPDATE ... SET deleted = 1`） |
| 单条查询                                      | `selectOneById(id)`、`selectOneByQuery(wrapper)`                                    |
| 列表查询                                      | `selectListByQuery(wrapper)`                                                        |
| 投影查询（映射到非 PO 类型）                  | `selectListByQueryAs(wrapper, Xxx.class)`、`paginateAs(page, wrapper, Xxx.class)`   |
| 分页                                          | `paginate(Page.of(pageNum, pageSize), wrapper)`                                     |
| 存在 / 计数                                   | `selectCountByQuery(wrapper)`                                                       |

> `update(po)` 默认忽略 `null` 字段。若聚合根业务方法会把某字段**置空**（如清除取消原因），需在 RepositoryImpl 中用 `UpdateChain` / `UpdateWrapper` 显式 `set(column, null)`。

### 1.9 Converter 转换类规范

Converter 负责 PO 与聚合根 / 实体之间的**纯技术转换**，使用 MapStruct。

#### 命名规范

| 规则项   | 规范                                                                 | 示例             |
|----------|----------------------------------------------------------------------|------------------|
| 类名     | `{业务名}Converter`，MapStruct 接口                                  | `OrderConverter` |
| 注解     | `@Mapper(componentModel = "spring")`                                 | —                |
| 存放位置 | `infrastructure/{业务名}/converter/`                                 | —                |
| 方法命名 | `toEntity`（PO → 实体）、`toPO`（实体 → PO）、`toAggregate`（PO → 聚合根）、`toEntityList`、`toAggregateList` | —                |
| 注入方式 | 构造注入到 RepositoryImpl                                            | —                |

#### 设计原则

- 只做**字段映射**，**禁止**业务判断
- **两级映射**：PO ↔ **实体**是主映射（`toEntity` / `toPO`），`id`、`version`、`createTime`、`updateTime` 在实体与 PO 上同名自动映射；
  `toAggregate(po)` 只负责把根实体装进聚合根：`@Mapping(target = "order", source = "po")`，MapStruct 会自动复用 `toEntity`
- `toPO` 的入参是 **根实体**（`toPO(OrderEntity order)`），RepositoryImpl 传 `aggregate.getOrder()`；不要写 `toPO(OrderAggregate)`
- 值对象：PO → 实体用 `default` 方法组装 `record`；实体 → PO 用 `@Mapping(source = "receiver.province")` 平铺；单列值对象（`PasswordValue`
  ↔ `passwordHash`）用一对 `default` 方法互转
- 子实体列表由 RepositoryImpl 单独查询后 `set` 到聚合根，Converter 的 `toAggregate` 对 `items` 声明 `ignore = true`
- 枚举无需手写转换：PO 与实体使用同一个领域枚举类型
- 类名以 `Converter` 结尾，**不能**叫 `XxxMapper`（与 MyBatis-Flex Mapper 冲突）

#### 与 Assembler 的区别

| 维度         | Converter（Infrastructure） | Assembler（Application）                          |
|--------------|-----------------------------|---------------------------------------------------|
| **转换对象** | PO ↔ 聚合根 / 实体          | RequestDTO / ResponseDTO ↔ Param / Query / 聚合根 |
| **所在层**   | Infrastructure              | Application                                       |
| **职责**     | 持久化映射                  | 接口模型映射                                      |

#### 完整代码示例

```java
package com.florian.sun.spring.template.infrastructure.order.converter;

/**
 * 订单数据转换器
 * PO ↔ 聚合根 / 实体，纯字段映射
 */
@Mapper(componentModel = "spring")
public interface OrderConverter {

    /** PO → 根实体；id / version / createTime / updateTime 同名自动映射 */
    @Mapping(target = "receiver", source = "po")
    OrderEntity toEntity(OrderPO po);

    /** PO → 聚合根：把根实体装进容器；子实体由 RepositoryImpl 另行加载 */
    @Mapping(target = "order", source = "po")
    @Mapping(target = "items", ignore = true)
    OrderAggregate toAggregate(OrderPO po);

    List<OrderAggregate> toAggregateList(List<OrderPO> poList);

    /** 值对象组装：MapStruct 会自动把它用于 receiver 字段 */
    default AddressValue toAddressValue(OrderPO po) {
        return new AddressValue(
            po.getReceiverProvince(), po.getReceiverCity(), po.getReceiverDetail(),
            po.getReceiverName(), po.getReceiverPhone());
    }

    /** 根实体 → PO；值对象平铺；deleted 由 MyBatis-Flex 逻辑删除维护 */
    @Mapping(target = "receiverProvince", source = "receiver.province")
    @Mapping(target = "receiverCity", source = "receiver.city")
    @Mapping(target = "receiverDetail", source = "receiver.detail")
    @Mapping(target = "receiverName", source = "receiver.receiverName")
    @Mapping(target = "receiverPhone", source = "receiver.receiverPhone")
    @Mapping(target = "deleted", ignore = true)
    OrderPO toPO(OrderEntity order);

    List<OrderItemEntity> toEntityList(List<OrderItemPO> poList);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "orderId", source = "orderId")
    OrderItemPO toItemPO(OrderItemEntity entity, Long orderId);
}
```

### 1.10 Redis 缓存规范（可选）

| 规则项 | 规范                                                                            |
|--------|---------------------------------------------------------------------------------|
| 位置   | `infrastructure/{业务名}/cache/{聚合根名}Cache`，被同业务域 RepositoryImpl 调用 |
| 技术   | `StringRedisTemplate` + Jackson 序列化聚合根                                    |
| 策略   | Cache-Aside：读先查缓存，未命中查库回填；写先更新库，再删除缓存                 |
| Key    | `{应用名}:{业务名}:{聚合根}:{id}`，如 `spring-template:order:aggregate:1001`    |
| 过期   | 必须设置 TTL，禁止永久 key                                                      |
| 禁止   | domain / application 感知缓存的存在；把 PO 放进缓存                             |

### 1.11 领域事件（可选）

需要"订单支付成功后发通知"这类事务后动作时，RepositoryImpl 在 `save` 成功后通过 `ApplicationEventPublisher` 发布 Spring 事件，`adaptor/{业务名}/input/{业务名}EventListener` 用 `@TransactionalEventListener` 消费。事件类定义在 `domain/{业务名}/model/event/`（record），只含 ID 与必要字段。

### 1.12 Sa-Token 权限数据源 StpInterfaceImpl

Sa-Token 通过 `StpInterface` 获取账号的权限码、角色码。它是**框架回调**，需要读库，因此放在 `infrastructure/auth/`，通过 domain 的 `UserRepository` 接口取数据。

```java
package com.florian.sun.spring.template.infrastructure.auth;

/**
 * Sa-Token 权限数据源
 * 角色码取自 UserRoleEnum.name()，供 @SaCheckRole 使用；本模板不使用细粒度权限码
 * 每次 @SaCheckPermission / @SaCheckRole 都会调用，数据量大时请在此处加缓存
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserRepository userRepository;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return userRepository.findById(Long.valueOf(loginId.toString()))
                .map(user -> List.of(user.getUser().getRole().name()))
                .orElse(List.of());
    }
}
```

对应的 domain 接口：

```java
public interface UserRepository {

    void save(UserAggregate user);

    Optional<UserAggregate> findById(Long id);

    Optional<UserAggregate> findByEmail(String email);

    boolean existsByEmail(String email);

    PageResult<UserAggregate> pageUsers(PageUserQuery query);
}
```

> 需要细粒度权限（`@SaCheckPermission`）时，扩展为 `t_role` / `t_permission` 表，在 `UserRepository` 增加
> `listPermissionCodes(Long userId)` 并在 `getPermissionList` 中返回；Sa-Token 侧无需改动。

---

## 二、写模式 infrastructure 规范

### 2.1 核心职责

- **save**：判断 `order.isNew()` 决定 `insertSelective` 还是 `update`；处理子实体的增删改；把 `id` 与 `version` 回填到 **根实体**
- **findById**：加载聚合根及其子实体，返回 `Optional`
- **remove**：逻辑删除主表与子表
- 乐观锁冲突翻译为 `BizException(CONCURRENT_CONFLICT)`

### 2.2 返回值规范

与 domain 层 Repository 接口保持一致：

| 方法       | 返回值             | 说明                                                                                                  |
|------------|--------------------|-------------------------------------------------------------------------------------------------------|
| `save`     | `void`             | 新增后 `root.setId(po.getId())`、`root.setVersion(0)`；更新后 `root.setVersion(version + 1)`（`root = aggregate.getOrder()`） |
| `findById` | `Optional<聚合根>` | 不存在返回 `Optional.empty()`                                                                         |
| `remove`   | `void`             | 逻辑删除                                                                                              |

### 2.3 完整代码示例

```java
package com.florian.sun.spring.template.infrastructure.order.repository;

import static com.florian.sun.spring.template.infrastructure.order.mysql.po.table.OrderItemTableDef.ORDER_ITEM;
import static com.florian.sun.spring.template.infrastructure.order.mysql.po.table.OrderTableDef.ORDER;
import static com.florian.sun.spring.template.infrastructure.user.mysql.po.table.UserTableDef.USER;

/**
 * 订单仓储实现（写模式 + 读模式）
 * 纯技术转换，无业务逻辑
 */
@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderConverter orderConverter;

    @Override
    public void save(OrderAggregate order) {
        // 持久化元数据（id / version）都在根实体上，PO 也从根实体转换
        OrderEntity root = order.getOrder();
        OrderPO po = orderConverter.toPO(root);
        if (order.isNew()) {
            // 1. 新增主表，主键回填到根实体
            orderMapper.insertSelective(po);
            root.setId(po.getId());
            root.setVersion(0);
        } else {
            // 2. 更新主表：MyBatis-Flex 自动追加 WHERE version = ? 并 SET version = version + 1
            int rows = orderMapper.update(po);
            BizAssert.isTrue(rows == 1, CommonErrorCode.CONCURRENT_CONFLICT);
            root.setVersion(root.getVersion() + 1);
        }
        // 3. 同步子实体
        saveItems(order);
    }

    private void saveItems(OrderAggregate order) {
        List<Long> keptIds = order.getItems().stream()
            .map(OrderItemEntity::getId)
            .filter(Objects::nonNull)
            .toList();

        // 3.1 删除聚合中已不存在的子实体（逻辑删除）
        QueryWrapper removed = QueryWrapper.create().where(ORDER_ITEM.ORDER_ID.eq(order.getId()));
        if (!keptIds.isEmpty()) {
            removed.and(ORDER_ITEM.ID.notIn(keptIds));
        }
        orderItemMapper.deleteByQuery(removed);

        // 3.2 新增 / 更新
        for (OrderItemEntity item : order.getItems()) {
            OrderItemPO itemPO = orderConverter.toItemPO(item, order.getId());
            if (item.getId() == null) {
                orderItemMapper.insertSelective(itemPO);
                item.setId(itemPO.getId());
            } else {
                orderItemMapper.update(itemPO);
            }
        }
    }

    @Override
    public Optional<OrderAggregate> findById(Long id) {
        OrderPO po = orderMapper.selectOneById(id);
        if (po == null) {
            return Optional.empty();
        }
        OrderAggregate order = orderConverter.toAggregate(po);
        List<OrderItemPO> itemPOs = orderItemMapper.selectListByQuery(
            QueryWrapper.create().where(ORDER_ITEM.ORDER_ID.eq(id)));
        order.setItems(orderConverter.toEntityList(itemPOs));
        return Optional.of(order);
    }

    @Override
    public Optional<OrderAggregate> findByOrderNo(String orderNo) {
        OrderPO po = orderMapper.selectOneByQuery(QueryWrapper.create().where(ORDER.ORDER_NO.eq(orderNo)));
        return po == null ? Optional.empty() : findById(po.getId());
    }

    @Override
    public boolean existsByOrderNo(String orderNo) {
        return orderMapper.selectCountByQuery(QueryWrapper.create().where(ORDER.ORDER_NO.eq(orderNo))) > 0;
    }

    @Override
    public void remove(OrderAggregate order) {
        orderItemMapper.deleteByQuery(QueryWrapper.create().where(ORDER_ITEM.ORDER_ID.eq(order.getId())));
        orderMapper.deleteById(order.getId());
    }

    // 读模式方法见第三章
}
```

> `save` 中主表与子表的多次写操作由 AppService 的 `@Transactional` 包住，任何一步抛异常整体回滚。RepositoryImpl 自身不开事务。

---

## 三、读模式 infrastructure 规范

### 3.1 核心职责

- **仅做数据查询和格式转换**，无业务逻辑
- 列表 / 分页查询可以不加载子实体
- 与聚合根结构差异大的列表投影，用 `selectListByQueryAs` / `paginateAs` 直接映射到 domain 的 `{方法名}Result`

### 3.2 返回值规范

| 方法类型 | 返回值                                      | 说明                                            |
|----------|---------------------------------------------|-------------------------------------------------|
| 单条查询 | `Optional<聚合根>`                          | 未找到返回 `Optional.empty()`                   |
| 列表查询 | `List<聚合根>`                              | 无数据返回空列表                                |
| 分页查询 | `PageResult<聚合根>` / `PageResult<Result>` | `Page` 不能穿透到 domain，必须转成 `PageResult` |

### 3.3 完整代码示例

```java
// 续 OrderRepositoryImpl

    @Override
    public PageResult<OrderAggregate> pageByBuyer(PageOrderQuery query) {
        // QueryWrapper 对 null 参数自动忽略条件，status 为空即查全部
        QueryWrapper wrapper = QueryWrapper.create()
            .where(ORDER.BUYER_ID.eq(query.getBuyerId()))
            .and(ORDER.STATUS.eq(query.getStatus()))
            .orderBy(ORDER.ID.desc());

        Page<OrderPO> page = orderMapper.paginate(Page.of(query.getPageNum(), query.getPageSize()), wrapper);
        return toPageResult(page, orderConverter.toAggregateList(page.getRecords()));
    }

    @Override
    public PageResult<OrderListItemResult> pageForAdmin(PageOrderQuery query) {
        // 投影查询：join 用户表取昵称，直接映射到 domain 的 Result，别名与 Result 字段名一致
        QueryWrapper wrapper = QueryWrapper.create()
            .select(ORDER.ID, ORDER.ORDER_NO, ORDER.STATUS, ORDER.TOTAL_AMOUNT, ORDER.CREATE_TIME,
                    USER.NICKNAME.as("buyerNickname"))
            .from(ORDER)
            .leftJoin(USER).on(ORDER.BUYER_ID.eq(USER.ID))
            .where(ORDER.STATUS.eq(query.getStatus()))
            .orderBy(ORDER.ID.desc());

        Page<OrderListItemResult> page = orderMapper.paginateAs(
            Page.of(query.getPageNum(), query.getPageSize()), wrapper, OrderListItemResult.class);
        return toPageResult(page, page.getRecords());
    }

    private static <T> PageResult<T> toPageResult(Page<?> page, List<T> records) {
        return PageResult.of(page.getPageNumber(), page.getPageSize(), page.getTotalRow(), records);
    }
```

> `pageForAdmin` 跨表 join 属于**查询投影**，是读模式允许的做法；写模式下加载聚合根仍以聚合边界为单位，不 join 其他聚合。

---

## 四、规则+计算模式 infrastructure 规范

### 4.1 核心职责

- **加载规则聚合根集合**：从数据库加载启用的规则，转换为规则聚合根
- 规则数据通常**只读**，管理后台的增删改按写模式另行实现
- 规则变化不频繁时可在 `cache/` 中加 Redis 缓存

### 4.2 返回值规范

| 方法类型       | 返回值             |
|----------------|--------------------|
| 查询启用规则   | `List<规则聚合根>` |
| 按条件查询规则 | `List<规则聚合根>` |

### 4.3 完整代码示例

```java
package com.florian.sun.spring.template.infrastructure.coupon.repository;

import static com.florian.sun.spring.template.infrastructure.coupon.mysql.po.table.CouponRuleTableDef.COUPON_RULE;

/**
 * 优惠规则仓储实现（规则+计算模式）
 * 加载规则并转换为聚合根，无业务逻辑
 */
@Repository
@RequiredArgsConstructor
public class CouponRuleRepositoryImpl implements CouponRuleRepository {

    private final CouponRuleMapper couponRuleMapper;
    private final CouponRuleConverter couponRuleConverter;

    @Override
    public List<CouponRuleAggregate> listEnabled() {
        List<CouponRulePO> poList = couponRuleMapper.selectListByQuery(
            QueryWrapper.create()
                .where(COUPON_RULE.ENABLED.eq(true))
                .orderBy(COUPON_RULE.PRIORITY.desc()));
        return couponRuleConverter.toAggregateList(poList);
    }

    @Override
    public Optional<CouponRuleAggregate> findById(Long id) {
        return Optional.ofNullable(couponRuleMapper.selectOneById(id)).map(couponRuleConverter::toAggregate);
    }

    @Override
    public void save(CouponRuleAggregate aggregate) {
        CouponRuleEntity root = aggregate.getRule();
        CouponRulePO po = couponRuleConverter.toPO(root);
        if (aggregate.isNew()) {
            couponRuleMapper.insertSelective(po);
            root.setId(po.getId());
            root.setVersion(0);
        } else {
            BizAssert.isTrue(couponRuleMapper.update(po) == 1, CommonErrorCode.CONCURRENT_CONFLICT);
            root.setVersion(root.getVersion() + 1);
        }
    }
}
```

```java
package com.florian.sun.spring.template.infrastructure.coupon.converter;

/**
 * 优惠规则转换器
 * 单表聚合：toEntity 做主映射，toAggregate 只负责装进容器
 */
@Mapper(componentModel = "spring")
public interface CouponRuleConverter {

    CouponRuleEntity toEntity(CouponRulePO po);

    @Mapping(target = "rule", source = "po")
    CouponRuleAggregate toAggregate(CouponRulePO po);

    List<CouponRuleAggregate> toAggregateList(List<CouponRulePO> poList);

    @Mapping(target = "deleted", ignore = true)
    CouponRulePO toPO(CouponRuleEntity rule);
}
```
