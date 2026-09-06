---
name: rename-spring-template
description: 为本 Spring Boot/Maven 模板及其派生项目改包、重命名项目或初始化二次开发工程，同步 Java 包与目录、Maven 坐标、启动类、应用配置、数据库名、S3 bucket 和规范文档。用户提出改包、修改根包名、重命名项目、基于模板初始化新项目时使用；不用于业务功能开发或真实基础设施创建。
---

# 改包与项目初始化

将当前工程的项目标识迁移为调用者指定的名称，保留业务结构与行为。默认执行下述完整初始化；用户明确限定范围或覆盖某个名称时，以该要求为准。

## 调用参数

必须明确获得以下三项，不从其中一项猜测另一项。缺失时一次性询问所有缺少的参数，先完成只读检查，补齐后再修改。

| 参数          | 含义                     | 示例                |
|---------------|--------------------------|---------------------|
| `groupId`     | 项目自身的 Maven groupId | `com.example`       |
| `artifactId`  | Maven artifactId         | `order-service`     |
| `basePackage` | Java 根包                | `com.example.order` |

未单独指定的名称按下表生成。将现有值、新值及推导来源列成映射表后执行；完整的改包请求就是对这些本地修改的授权，不重复索要执行确认。仅请求预览时只展示映射及受影响文件。

| 名称                      | 默认值                                                     | 上述示例的结果                 |
|---------------------------|------------------------------------------------------------|--------------------------------|
| Maven name、Spring 应用名 | `artifactId`                                               | `order-service`                |
| Maven description         | `artifactId` + ` 项目`                                     | `order-service 项目`           |
| 启动类                    | `artifactId` 按连字符分词转 PascalCase，再加 `Application` | `OrderServiceApplication`      |
| 启动测试类                | 新启动类名 + `Tests`                                       | `OrderServiceApplicationTests` |
| OpenAPI 标题              | `artifactId` + ` API`                                      | `order-service API`            |
| 数据库名                  | 将 `artifactId` 的连字符换为下划线                         | `order_service`                |
| S3 / MinIO bucket         | `artifactId`                                               | `order-service`                |

在写入前校验全部目标值：

- Java 根包每段和启动类名必须是当前 JDK 接受的标识符，排除关键字、空段、路径分隔符和通配符；不能将包名中的点直接作为文件名的一部分。优先采用小写根包和
  PascalCase 类名。
- Maven 坐标须为合法的字面坐标，不能包含空白、路径分隔符或未解析的 `${...}`。不要把 Maven 坐标的规则直接当作 Java 标识符规则。
- 数据库名须能合法用于 MySQL 标识符，长度不超过 64 个字符；SQL 中按需要使用反引号引用并转义反引号，JDBC URL 中正确编码库名，保留其他
  URL 部分。默认推导优先使用小写字母、数字和下划线。
- bucket 须满足目标存储的命名要求；按 S3 通用桶规则检查 3-63 字符、小写字母/数字/点/连字符、字母或数字首尾、非 IP
  格式、无连续点及服务保留前后缀。本地只能验证名称格式，不能声称云端名称可用。
- 默认推导生成非法值或存在歧义时，列出需覆盖的名称并一次性询问，不静默删字符、加前缀或另选名字。例如 `123-order`
  不能直接生成合法启动类，含下划线的 artifactId 不能直接作为 S3 bucket。

调用示例：

```text
使用 $rename-spring-template 初始化项目：
groupId=com.example
artifactId=order-service
basePackage=com.example.order
```

## 1. 识别当前工程

以当前仓库为作用域，先读取适用的 `AGENTS.md`、`git status --short` 和相关 diff，保留已有修改。核对以下入口，实际值以本次读取为准：

| 入口                              | 识别内容                                                                                |
|-----------------------------------|-----------------------------------------------------------------------------------------|
| `pom.xml`                         | 项目直接子节点的坐标、名称、描述，Java 版本、构建插件和注解处理器                       |
| `src/main/java`、`src/test/java`  | `@SpringBootApplication` 所在包及启动类、关联测试类、全部待迁移文件                     |
| Java 配置与引用                   | `@MapperScan`、其他包扫描注解、反射字符串、静态导入和显式主类配置                       |
| `src/main/resources/application*` | 应用名、JDBC 数据库名、S3 bucket；如有 profile 或环境变量占位符，识别其默认值和覆盖关系 |
| `docs/sql/schema.sql`             | 建库、选库语句和项目名称注释                                                            |
| `docs/rule` 及其他项目文档        | 包声明示例、目录树、项目标题、配置片段和 Redis key 前缀                                 |
| `mybatis-flex.config`             | APT 输出规则；如果有显式包名，同步处理                                                  |

初始模板使用 `com.florian.sun.spring.template`、`SpringTemplateApplication`、`spring-template` 和 `spring_template`
。这些只用于理解模板，不作为固定替换源：再次改包必须读取上次初始化后的实际值。

确定旧根包时综合启动类包声明和源码目录，不通过 `groupId + artifactId` 推测。若存在多个启动入口、模块或互不包含的业务根包，先说明候选范围，获得明确选择后再迁移。

列出主源码、测试源码的完整“源文件 -> 目标文件”映射，提前检查类名冲突、目标文件占用和 Windows
大小写冲突。目标目录存在本身不算冲突；不同源文件落到同一目标或目标已有其他文件才需停止并解决。源与目标相同则跳过。

## 2. 迁移源码和目录

- 按 Java 名称边界替换旧根包：覆盖根包本身及其子包的 `package`、普通/静态 `import`、全限定名、扫描或反射字符串。不要误改仅共享文本前缀的其他包。
- 同时迁移 `src/main/java` 与 `src/test/java` 下的文件到新包路径，保留相对于旧根包的业务子目录。使用预先计算的文件清单，避免新旧包互相嵌套时递归移动刚生成的目录。
- 重命名启动类及关联启动测试类的声明、文件名和引用，包括 `SpringApplication.run(...)`、测试中的显式 `classes`
  引用、文档树和存在的构建主类配置。默认让启动类继续位于业务根包，保留组件扫描覆盖范围。
- 本项目的 `MyBatisFlexConfig` 使用
  `@MapperScan(basePackages = "旧根包.infrastructure", markerInterface = BaseMapper.class)`，更新包前缀并保留
  `markerInterface`，避免把 MapStruct 接口注册成 MyBatis Mapper。
- 仓储中的 `UserTableDef`、`FileTableDef` 等静态导入必须同步。MyBatis-Flex 的 TableDef 和 MapStruct 实现是 APT 生成物，通过
  clean 构建再生成，不手工改 `target` 中的代码；保留 Lombok/MapStruct 处理器配置及顺序。
- Windows 上先解析并核对移动/清理路径位于当前仓库的预期源码根目录，使用原生文件操作和字面路径，不拼接跨 shell
  的移动或删除命令。文件移动完成后只清理已空的旧包目录，不递归删除旧包树，不删除源码根目录。

## 3. 更新项目标识与配置

- 用 XML 解析器定位 `pom.xml` 的项目直接子节点，更新 `groupId`、`artifactId`、`name`、`description`，注意 Maven 默认命名空间。保留
  parent、依赖、插件坐标和版本；不要全局替换旧 groupId。若已有属性引用或继承关系，沿实际引用定位修改点。
- 按 YAML 键路径更新 `spring.application.name`、`spring.datasource.url` 的库名以及 `file.storage.s3.bucket`
  。优先使用可保留注释的结构化编辑；精准补丁也必须先核对键路径。保留连接地址、查询参数、账号、密钥及环境变量名，仅按映射更新项目相关默认值。
- 更新 `OpenApiConfig` 的 `Info.title`，保留 API 版本和鉴权方案。
- 同步 SQL 中的 `CREATE DATABASE`、`USE` 和项目名注释，确认选库名与 JDBC 连接的数据库名一致；保留建表结构、种子数据和现有业务表名。
- 保留原文件编码、换行与无关注释。不同字段虽有相同旧文本，也须按各自映射更新，例如独立覆盖的 Maven name、应用名和 bucket
  不能互相覆盖。

## 4. 同步文档并检查残留

更新 `docs/rule` 中的包声明、导入、根包目录树、启动类文件名、项目标题、数据库和 OpenAPI 配置示例，以及 Redis key
中的应用名前缀。检查其他已纳入项目的文档与配置是否也引用旧标识；如果已有 `.cursor/rules` 等规范副本，也同步其项目示例。

以当前映射中的旧值搜索点分包名、正反斜杠包路径、类名、项目名和数据库名。检查每项命中的用途后修改，不进行无边界的全仓文本替换。搜索源码、配置、SQL
和项目文档时排除 `.git`、IDE 缓存、构建产物及无关二进制文件；不要遗漏未忽略的新文件或实际维护的规则目录。

本 skill 的名称和模板历史示例保持稳定，不计为改包残留。对于新旧值相同、新包包含旧包前缀、第三方坐标或仍被明确保留的旧值，按语义判断命中，并在结果中说明合理保留项；不要为了“搜索零命中”破坏有效内容。

## 5. 验证与交付

优先使用当前仓库的 Maven Wrapper 和满足 `pom.xml` 要求的 JDK，不将作者本机的绝对工具路径写入项目。先检查实际 Java 版本。

在 Windows PowerShell 中运行以下命令，将示例测试类替换为实际新启动测试类；整个 `-Dtest` 参数须作为一个参数传递：

```powershell
java -version
.\mvnw.cmd clean verify "-Dtest=*,!OrderServiceApplicationTests"
```

其他系统使用 `./mvnw clean verify '-Dtest=*,!OrderServiceApplicationTests'`。

上述命令显式排除当前模板中依赖外部服务的启动上下文测试，仍编译全部测试并运行其余单元测试。若派生工程新增了集成测试，先识别其运行阶段与外部依赖，调整为明确的验证集合并报告排除项；不能盲目假定一次
`-Dtest` 设置覆盖 Failsafe 等其他测试插件。

MySQL、Redis、S3 / MinIO 等实际依赖已满足时，再执行不带测试排除参数的 clean
verify；需要核查部署后的扫描或连接行为时运行应用。缺少依赖则报告启动上下文/运行时验证未完成，不通过删除测试、修改业务逻辑或伪造配置来掩盖问题。

完成以下核对：

- clean 构建成功，新的 TableDef 和 MapStruct 实现在新包下生成，无旧包编译错误。
- 新启动类与测试类的文件名、声明及引用一致，包声明与文件路径匹配，源码数量没有意外变化。
- 打包产物的 Maven 坐标及 Spring Boot `Start-Class` 指向新项目、新启动类。
- Maven、应用配置、SQL 和文档符合映射，旧目录中没有遗漏源码；对剩余旧标识逐项说明。
- 检查 `git diff --check`、`git diff --stat`、`git status --short` 与实际
  diff，审阅删除和新增文件以确认目录迁移，保留原有用户修改。未跟踪的新目标文件也必须读取检查，不为制造重命名展示而擅自暂存文件。

交付说明包含实际新旧映射、主要修改范围、已执行命令及结果、跳过或失败的验证、需要调用者准备的数据库/bucket。构建失败时区分改包问题与工具/外部依赖问题，处理已授权范围内的问题后如实报告剩余阻塞，不宣称全部验证通过。

## 操作边界

完整初始化只涉及仓库内的名称与配置。除非另有明确要求，不创建或操作真实数据库和 bucket，不执行 SQL，不迁移数据，不重命名仓库根目录，不修改
Git remote，不提交或推送；保留版本、依赖、业务表结构、账号密钥及作者信息。对本地配置的改名不能描述成外部资源已创建或已可连接。
