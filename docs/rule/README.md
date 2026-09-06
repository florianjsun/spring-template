## spring-template DDD 落地规范

一套面向 **个人项目 / 单体 Spring Boot 应用**的 DDD（领域驱动设计）落地规范，基于六边形架构。

解决的问题： **知道 DDD 理论，但不知道在这套技术栈下代码该怎么写、放在哪里。**

## 特性

- **六边形架构**：依赖倒置，技术细节（ORM、鉴权、HTTP）与业务逻辑彻底分离
- **4 层 + common**：Adaptor、Application、Domain、Infrastructure，外加共享内核 common，比 6 层更轻
- **四种开发模式**：写模式、读模式、纯计算模式、规则+计算模式，附决策树
- **异常驱动**：领域层抛 `BizException`，全局异常处理器统一转 `Result<T>`
- **技术栈落地**：每一层都给出 MyBatis-Flex、Sa-Token、MapStruct、Validation 的具体用法与边界
- **AI 友好**：每个文件带 Cursor Rules frontmatter，可直接拷入 `.cursor/rules/`
- **通用工具**：直接使用 `hutool-all` 中的 API，避免重复实现或仅做转发封装；详见 [Hutool 工具使用规范](ddd-common-layer.md#八hutool-工具使用规范)

## 文档结构

| 文件                                                       | 说明                                                                                                             |
|------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| [DDD.md](DDD.md)                                           | **总览**：架构原则、依赖规则、完整包结构、四种开发模式、模式选择决策树、技术栈映射                               |
| [ddd-common-layer.md](ddd-common-layer.md)                 | **共享内核规范**：`Result`、`ErrorCode`、`BizException`、`BaseAggregate`、校验分组、`config/` 配置类模板         |
| [ddd-domain-layer.md](ddd-domain-layer.md)                 | **领域层规范**：聚合根、实体、值对象（record）、Param/Result、领域服务、仓储接口、乐观锁                         |
| [ddd-application-layer.md](ddd-application-layer.md)       | **应用层规范**：场景编排、CQRS、`@Transactional`、DTO + Validation、MapStruct Assembler、Adaptor 接口            |
| [ddd-adaptor-layer.md](ddd-adaptor-layer.md)               | **适配器层规范**：Controller + Sa-Token + SpringDoc、定时任务、Output Adaptor、`GlobalExceptionHandler`          |
| [ddd-infrastructure-layer.md](ddd-infrastructure-layer.md) | **基础设施层规范**：RepositoryImpl、MyBatis-Flex PO/Mapper/QueryWrapper、MapStruct Converter、`StpInterfaceImpl` |

## 快速开始

### 作为架构参考

1. 先读 [DDD.md](DDD.md)，了解分层、依赖规则和四种开发模式
2. 用决策树判断当前需求属于哪种模式
3. 按 DDD.md 第四章的"规范文件组合"逐层实现，直接复制各层的代码模板改名即可

### 作为 AI 编程规则

将本目录下的 `.md` 文件复制到项目 `.cursor/rules/` 目录，AI 生成代码时会自动遵循这套分层与命名。文件头部的 frontmatter 已按
Cursor Rules 格式编写，可按需调整 `alwaysApply`。

## 技术栈映射

| easy-DDD 中的概念                     | 本项目实现                                                                                 |
|---------------------------------------|--------------------------------------------------------------------------------------------|
| `ResultDO` 全链路返回                 | 领域层/应用层抛 `BizException`；`adaptor/common/GlobalExceptionHandler` 统一转 `Result<T>` |
| `AggregateException` + `BizException` | 统一为 `BizException(ErrorCode)`                                                           |
| `LevelLock` 分布式锁                  | MyBatis-Flex `@Column(version = true)` 乐观锁；分布式锁按需引入                            |
| `Field<T>` / `FieldSet<T>` 实体字段   | 普通字段 + Lombok `@Getter/@Setter`，业务修改只走业务方法                                  |
| `BaseValue` 值对象                    | Java `record`                                                                              |
| `client` 层（HSF 接口契约）           | 去掉；DTO 放 `application/{biz}/dto`，HTTP 由 `adaptor/{biz}/input` 暴露                   |
| `model` 层（共享模型）                | `common/` 共享内核                                                                         |
| MyBatis XML Mapper                    | MyBatis-Flex `BaseMapper<PO>` + `QueryWrapper` + APT `TableDef`                            |
| 手写 Converter / Assembler            | MapStruct `@Mapper(componentModel = "spring")`                                             |
| `requestDTO.check()` 自校验           | Jakarta Validation 注解 + Controller `@Valid`                                              |
| HSF / MetaQ / ScheduleX 入口          | Controller / Spring `@Scheduled` / Spring `@EventListener`                                 |
| 登录态、鉴权                          | Sa-Token（`StpUtil`、`@SaCheckPermission`、`StpInterface`）                                |

## 与 easy-DDD 的主要差异

| 维度                       | easy-DDD                                                                 | 本规范                                                |
|----------------------------|--------------------------------------------------------------------------|-------------------------------------------------------|
| 分层数                     | 6 层（domain / application / adaptor / infrastructure / client / model） | 4 层 + common                                         |
| 错误处理                   | 各层返回 `ResultDO`，禁止抛异常                                          | 抛 `BizException`，全局统一捕获；事务可自然回滚       |
| AppService / DomainService | interface + Impl                                                         | 直接写具体类；只有 `Repository`、`Adaptor` 必须是接口 |
| 并发控制                   | 分布式锁 `LevelLock`                                                     | 乐观锁 `version` 字段                                 |
| 参数校验                   | DTO `check()` 方法                                                       | Jakarta Validation 注解                               |
| 事务边界                   | 未约定                                                                   | AppService 命令方法 `@Transactional`                  |
| 序列化注解                 | fastjson `@JSONField`                                                    | 聚合根不直接输出，无需注解                            |

## 工程结构（概览）

```plain
com.florian.sun.spring.template
├── config/            # Spring 配置类（SaToken、MyBatis-Flex、OpenAPI）
├── common/            # 共享内核：Result、异常、基类、枚举、校验分组、工具
├── domain/            # 领域层：聚合根、实体、值对象、领域服务、仓储接口
├── application/       # 应用层：场景编排、Assembler、DTO、Adaptor 接口
├── infrastructure/    # 基础设施层：仓储实现、PO、Mapper、Converter
└── adaptor/           # 适配器层：Controller / Job（input），第三方调用（output）
```

完整包结构见 [DDD.md 第二章](DDD.md#二工程结构)。

## 四种开发模式

| 模式              | 聚合根/实体    | 业务逻辑位置    | 修改状态 | 典型场景                     |
|-------------------|----------------|-----------------|----------|------------------------------|
| **写模式**        | 有             | 聚合根/实体方法 | 是       | 创建订单、取消订单、修改资料 |
| **读模式**        | 有（数据载体） | 无              | 否       | 订单详情、分页列表           |
| **规则+计算模式** | 有             | 聚合根/实体方法 | 否       | 优惠规则匹配与计算           |
| **纯计算模式**    | 无             | DomainService   | 否       | 费用试算、报表汇总           |

## 前置准备

本规范假定 `pom.xml` 中已存在以下依赖（当前脚手架已声明）：MyBatis-Flex `mybatis-flex-spring-boot4-starter` +
`mybatis-flex-processor`、Sa-Token `sa-token-spring-boot4-starter` + `sa-token-redis-template` + `sa-token-fastjson2`
、MapStruct `mapstruct` + `mapstruct-processor`、`spring-boot-starter-validation`、`springdoc-openapi-starter-webmvc-ui`
、Lombok、Hutool `hutool-all`（5.8.47）。

需要补充的一项：MapStruct 与 Lombok 同时使用时，`maven-compiler-plugin` 的 `annotationProcessorPaths` 中必须在 `lombok` 与
`mapstruct-processor` 之间加入 `org.projectlombok:lombok-mapstruct-binding`（0.2.0），否则 MapStruct 生成代码时看不到
Lombok 生成的 getter/setter。
