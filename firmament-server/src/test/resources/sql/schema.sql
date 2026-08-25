-- =============================================================================
-- 集成测试专用数据库 Schema（仅用于 Testcontainers MySQL 容器，不用于生产）
-- =============================================================================
-- 说明：
-- 1. 此文件仅在集成测试（profile = it）启动时由 spring.sql.init 加载，
--    在一次性 MySQL 8 容器内建表，测试结束容器即销毁，绝不触碰真实数据库。
-- 2. 表结构与 firmament-pojo 中的 11 个实体一一对应，
--    字段命名遵循 map-underscore-to-camel-case（实体驼峰 -> 数据库下划线）。
-- 3. 生产环境的真实 DDL 不在仓库内（由外部 MySQL 维护），这里是为测试自洽而重建。
-- 4. ⚠️ 该文件与实体之间没有自动校验：实体新增/重命名字段时必须同步修改这里，
--    否则集成测试会在运行时以 "Unknown column" 失败，或静默丢字段。
-- ==============================================================================

-- 员工表（管理端账号，用于 POST /admin/employee/login）
CREATE TABLE IF NOT EXISTS `employee`
(
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
  `name`        VARCHAR(32)           DEFAULT NULL COMMENT '姓名',
  `password`    VARCHAR(128) NOT NULL COMMENT '密码（{BCRYPT}/{MD5} 前缀）',
  `phone`       VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
  `sex`         VARCHAR(2)            DEFAULT NULL COMMENT '性别',
  `id_number`   VARCHAR(32)           DEFAULT NULL COMMENT '身份证号',
  `status`      INT                   DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time` DATETIME              DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME              DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT                DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT                DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '员工表';

-- 分类表（菜品分类 / 套餐分类）
CREATE TABLE IF NOT EXISTS `category`
(
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type`        INT                  DEFAULT NULL COMMENT '类型 1菜品 2套餐',
  `name`        VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort`        INT                  DEFAULT 0 COMMENT '排序',
  `status`      INT                  DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time` DATETIME             DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME             DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT               DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT               DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '分类表';

-- 菜品表
CREATE TABLE IF NOT EXISTS `dish`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64)   NOT NULL COMMENT '菜品名称',
  `category_id` BIGINT        NOT NULL COMMENT '分类id',
  `price`       DECIMAL(8, 2)          DEFAULT NULL COMMENT '价格',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT '图片',
  `description` VARCHAR(255)           DEFAULT NULL COMMENT '描述',
  `status`      INT                    DEFAULT 1 COMMENT '状态 1起售 0停售',
  `create_time` DATETIME               DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME               DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT                 DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT                 DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_dish_category_id` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '菜品表';

-- 菜品口味表
CREATE TABLE IF NOT EXISTS `dish_flavor`
(
  `id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` BIGINT      NOT NULL COMMENT '菜品id',
  `name`   VARCHAR(64)           DEFAULT NULL COMMENT '口味名称',
  `value`  VARCHAR(512)          DEFAULT NULL COMMENT '口味值（JSON 数组字符串）',
  PRIMARY KEY (`id`),
  KEY `idx_dish_flavor_dish_id` (`dish_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '菜品口味表';

-- 套餐表
CREATE TABLE IF NOT EXISTS `setmeal`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` BIGINT        NOT NULL COMMENT '分类id',
  `name`        VARCHAR(64)   NOT NULL COMMENT '套餐名称',
  `price`       DECIMAL(8, 2)          DEFAULT NULL COMMENT '价格',
  `status`      INT                    DEFAULT 1 COMMENT '状态 1启用 0停用',
  `description` VARCHAR(255)           DEFAULT NULL COMMENT '描述',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT '图片',
  `create_time` DATETIME               DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME               DEFAULT NULL COMMENT '更新时间',
  `create_user` BIGINT                 DEFAULT NULL COMMENT '创建人',
  `update_user` BIGINT                 DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  KEY `idx_setmeal_category_id` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '套餐表';

-- 套餐菜品关系表
CREATE TABLE IF NOT EXISTS `setmeal_dish`
(
  `id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` BIGINT       NOT NULL COMMENT '套餐id',
  `dish_id`   BIGINT        NOT NULL COMMENT '菜品id',
  `name`      VARCHAR(64)            DEFAULT NULL COMMENT '菜品名称（冗余）',
  `price`     DECIMAL(8, 2)          DEFAULT NULL COMMENT '菜品原价',
  `copies`    INT                    DEFAULT 1 COMMENT '份数',
  PRIMARY KEY (`id`),
  KEY `idx_setmeal_dish_setmeal_id` (`setmeal_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '套餐菜品关系表';

-- 用户表（C 端用户，用于 POST /user/user/phoneLogin）
CREATE TABLE IF NOT EXISTS `user`
(
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid`      VARCHAR(64)           DEFAULT NULL COMMENT '微信openid',
  `name`        VARCHAR(32)           DEFAULT NULL COMMENT '姓名',
  `phone`       VARCHAR(32)           DEFAULT NULL COMMENT '手机号',
  `password`    VARCHAR(128)          DEFAULT NULL COMMENT '密码（{BCRYPT}/{MD5} 前缀）',
  `sex`         VARCHAR(2)            DEFAULT NULL COMMENT '性别',
  `id_number`   VARCHAR(32)           DEFAULT NULL COMMENT '身份证号',
  `avatar`      VARCHAR(255)          DEFAULT NULL COMMENT '头像',
  `create_time` DATETIME              DEFAULT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户表';

-- 地址簿表
CREATE TABLE IF NOT EXISTS `address_book`
(
  `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`       BIGINT               DEFAULT NULL COMMENT '用户id',
  `consignee`     VARCHAR(32)          DEFAULT NULL COMMENT '收货人',
  `phone`         VARCHAR(32)          DEFAULT NULL COMMENT '手机号',
  `sex`           VARCHAR(2)           DEFAULT NULL COMMENT '性别',
  `province_code` VARCHAR(16)          DEFAULT NULL COMMENT '省级编号',
  `province_name` VARCHAR(32)          DEFAULT NULL COMMENT '省级名称',
  `city_code`     VARCHAR(16)          DEFAULT NULL COMMENT '市级编号',
  `city_name`     VARCHAR(32)          DEFAULT NULL COMMENT '市级名称',
  `district_code` VARCHAR(16)          DEFAULT NULL COMMENT '区级编号',
  `district_name` VARCHAR(32)          DEFAULT NULL COMMENT '区级名称',
  `detail`        VARCHAR(255)         DEFAULT NULL COMMENT '详细地址',
  `label`         VARCHAR(16)          DEFAULT NULL COMMENT '标签',
  `is_default`    INT                  DEFAULT 0 COMMENT '是否默认 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_address_book_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '地址簿表';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders`
(
  `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number`                 VARCHAR(64)            DEFAULT NULL COMMENT '订单号',
  `status`                 INT                    DEFAULT NULL COMMENT '订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消',
  `user_id`                BIGINT                 DEFAULT NULL COMMENT '用户id',
  `address_book_id`        BIGINT                 DEFAULT NULL COMMENT '地址id',
  `order_time`             DATETIME               DEFAULT NULL COMMENT '下单时间',
  `checkout_time`          DATETIME               DEFAULT NULL COMMENT '结账时间',
  `pay_method`             INT                    DEFAULT NULL COMMENT '支付方式 1微信 2支付宝',
  `pay_status`             INT                    DEFAULT NULL COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount`                 DECIMAL(10, 2)         DEFAULT NULL COMMENT '实收金额',
  `remark`                 VARCHAR(255)           DEFAULT NULL COMMENT '备注',
  `user_name`              VARCHAR(32)            DEFAULT NULL COMMENT '用户名',
  `phone`                  VARCHAR(32)            DEFAULT NULL COMMENT '手机号',
  `address`                VARCHAR(255)           DEFAULT NULL COMMENT '地址',
  `consignee`              VARCHAR(32)            DEFAULT NULL COMMENT '收货人',
  `cancel_reason`          VARCHAR(255)           DEFAULT NULL COMMENT '取消原因',
  `rejection_reason`       VARCHAR(255)           DEFAULT NULL COMMENT '拒单原因',
  `cancel_time`            DATETIME               DEFAULT NULL COMMENT '取消时间',
  `estimated_delivery_time` DATETIME              DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status`        INT                    DEFAULT NULL COMMENT '配送状态 1立即送出 0选择具体时间',
  `delivery_time`          DATETIME               DEFAULT NULL COMMENT '送达时间',
  `pack_amount`            INT                    DEFAULT 0 COMMENT '打包费',
  `tableware_number`       INT                    DEFAULT 0 COMMENT '餐具数量',
  `tableware_status`       INT                    DEFAULT NULL COMMENT '餐具数量状态 1按餐量 0具体数量',
  PRIMARY KEY (`id`),
  KEY `idx_orders_number` (`number`),
  KEY `idx_orders_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '订单表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_detail`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64)            DEFAULT NULL COMMENT '名称',
  `order_id`    BIGINT                 DEFAULT NULL COMMENT '订单id',
  `dish_id`     BIGINT                 DEFAULT NULL COMMENT '菜品id',
  `setmeal_id`  BIGINT                 DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` VARCHAR(64)            DEFAULT NULL COMMENT '口味',
  `number`      INT                    DEFAULT NULL COMMENT '数量',
  `amount`      DECIMAL(10, 2)         DEFAULT NULL COMMENT '金额',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT '图片',
  PRIMARY KEY (`id`),
  KEY `idx_order_detail_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '订单明细表';

-- 购物车表
CREATE TABLE IF NOT EXISTS `shopping_cart`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(64)            DEFAULT NULL COMMENT '名称',
  `user_id`     BIGINT                 DEFAULT NULL COMMENT '用户id',
  `dish_id`     BIGINT                 DEFAULT NULL COMMENT '菜品id',
  `setmeal_id`  BIGINT                 DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` VARCHAR(64)            DEFAULT NULL COMMENT '口味',
  `number`      INT                    DEFAULT NULL COMMENT '数量',
  `amount`      DECIMAL(10, 2)         DEFAULT NULL COMMENT '金额',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT '图片',
  `create_time` DATETIME               DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_shopping_cart_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '购物车表';
