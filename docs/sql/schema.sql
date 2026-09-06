-- spring-template 基础表结构
-- 约定见 docs/rule/ddd-infrastructure-layer.md 1.6 节：
-- 每张表必带 version / deleted / create_time / update_time，枚举存 TINYINT，不建物理外键

CREATE DATABASE IF NOT EXISTS spring_template DEFAULT CHARACTER SET utf8mb4;
USE spring_template;

-- ----------------------------------------------------------------------------
-- 用户
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    email          VARCHAR(128) NOT NULL COMMENT '邮箱，登录账号',
    password_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    nickname       VARCHAR(64)  NOT NULL COMMENT '昵称',
    avatar_file_id BIGINT       NULL COMMENT '头像文件ID，关联 t_file.id',
    role           TINYINT      NOT NULL DEFAULT 1 COMMENT '角色：1普通用户 2管理员',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
    version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

-- ----------------------------------------------------------------------------
-- 文件元数据（文件内容存 S3 / MinIO，storage_key 为对象 key）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_key   VARCHAR(255) NOT NULL COMMENT '对象存储 key：yyyy/MM/dd/{snowflake}.{ext}',
    content_type  VARCHAR(128) NOT NULL COMMENT 'MIME 类型',
    file_size     BIGINT       NOT NULL COMMENT '文件大小（字节）',
    uploader_id   BIGINT       NOT NULL COMMENT '上传者ID，关联 t_user.id',
    version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_storage_key (storage_key),
    KEY idx_uploader_id (uploader_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件';

-- ----------------------------------------------------------------------------
-- 种子数据：初始管理员
-- 账号 admin@example.com，密码 Admin123456（BCrypt，上线前请修改）
-- ----------------------------------------------------------------------------
INSERT INTO t_user (email, password_hash, nickname, role, status)
SELECT 'admin@example.com', '$2a$10$EKGX0TlGeII3t1rZV4e2POgsGhq0kRzM/k7GcqPkYJ1oAV9gcsG8G', '管理员', 2, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM t_user WHERE email = 'admin@example.com');
