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
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `username`    VARCHAR(50)  NOT NULL COMMENT 'login username',
  `name`        VARCHAR(32)           DEFAULT NULL COMMENT 'name',
  `password`    VARCHAR(128) NOT NULL COMMENT 'password ({BCRYPT}/{MD5} prefix)',
  `phone`       VARCHAR(32)           DEFAULT NULL COMMENT 'phone number',
  `sex`         VARCHAR(2)            DEFAULT NULL COMMENT 'gender',
  `id_number`   VARCHAR(32)           DEFAULT NULL COMMENT 'id card number',
  `status`      INT                   DEFAULT 1 COMMENT 'status 1=enabled 0=disabled',
  `create_time` DATETIME              DEFAULT NULL COMMENT 'create time',
  `update_time` DATETIME              DEFAULT NULL COMMENT 'update time',
  `create_user` BIGINT                DEFAULT NULL COMMENT 'creator',
  `update_user` BIGINT                DEFAULT NULL COMMENT 'updater',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'employee table';

-- 分类表（菜品分类 / 套餐分类）
CREATE TABLE IF NOT EXISTS `category`
(
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `type`        INT                  DEFAULT NULL COMMENT 'type 1=dish 2=setmeal',
  `name`        VARCHAR(64) NOT NULL COMMENT 'category name',
  `sort`        INT                  DEFAULT 0 COMMENT 'sort order',
  `status`      INT                  DEFAULT 1 COMMENT 'status 1=enabled 0=disabled',
  `create_time` DATETIME             DEFAULT NULL COMMENT 'create time',
  `update_time` DATETIME             DEFAULT NULL COMMENT 'update time',
  `create_user` BIGINT               DEFAULT NULL COMMENT 'creator',
  `update_user` BIGINT               DEFAULT NULL COMMENT 'updater',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'category table';

-- 菜品表
CREATE TABLE IF NOT EXISTS `dish`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `name`        VARCHAR(64)   NOT NULL COMMENT 'dish name',
  `category_id` BIGINT        NOT NULL COMMENT 'category id',
  `price`       DECIMAL(8, 2)          DEFAULT NULL COMMENT 'price',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT 'image',
  `description` VARCHAR(255)           DEFAULT NULL COMMENT 'description',
  `status`      INT                    DEFAULT 1 COMMENT 'status 1=on sale 0=off sale',
  `create_time` DATETIME               DEFAULT NULL COMMENT 'create time',
  `update_time` DATETIME               DEFAULT NULL COMMENT 'update time',
  `create_user` BIGINT                 DEFAULT NULL COMMENT 'creator',
  `update_user` BIGINT                 DEFAULT NULL COMMENT 'updater',
  PRIMARY KEY (`id`),
  KEY `idx_dish_category_id` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'dish table';

-- 菜品口味表
CREATE TABLE IF NOT EXISTS `dish_flavor`
(
  `id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `dish_id` BIGINT      NOT NULL COMMENT 'dish id',
  `name`   VARCHAR(64)           DEFAULT NULL COMMENT 'flavor name',
  `value`  VARCHAR(512)          DEFAULT NULL COMMENT 'flavor value (JSON array string)',
  PRIMARY KEY (`id`),
  KEY `idx_dish_flavor_dish_id` (`dish_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'dish flavor table';

-- 套餐表
CREATE TABLE IF NOT EXISTS `setmeal`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `category_id` BIGINT        NOT NULL COMMENT 'category id',
  `name`        VARCHAR(64)   NOT NULL COMMENT 'setmeal name',
  `price`       DECIMAL(8, 2)          DEFAULT NULL COMMENT 'price',
  `status`      INT                    DEFAULT 1 COMMENT 'status 1=enabled 0=disabled',
  `description` VARCHAR(255)           DEFAULT NULL COMMENT 'description',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT 'image',
  `create_time` DATETIME               DEFAULT NULL COMMENT 'create time',
  `update_time` DATETIME               DEFAULT NULL COMMENT 'update time',
  `create_user` BIGINT                 DEFAULT NULL COMMENT 'creator',
  `update_user` BIGINT                 DEFAULT NULL COMMENT 'updater',
  PRIMARY KEY (`id`),
  KEY `idx_setmeal_category_id` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'setmeal table';

-- 套餐菜品关系表
CREATE TABLE IF NOT EXISTS `setmeal_dish`
(
  `id`        BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `setmeal_id` BIGINT       NOT NULL COMMENT 'setmeal id',
  `dish_id`   BIGINT        NOT NULL COMMENT 'dish id',
  `name`      VARCHAR(64)            DEFAULT NULL COMMENT 'dish name (redundant)',
  `price`     DECIMAL(8, 2)          DEFAULT NULL COMMENT 'dish original price',
  `copies`    INT                    DEFAULT 1 COMMENT 'copies',
  PRIMARY KEY (`id`),
  KEY `idx_setmeal_dish_setmeal_id` (`setmeal_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'setmeal-dish relation table';

-- 用户表（C 端用户，用于 POST /user/user/phoneLogin）
CREATE TABLE IF NOT EXISTS `user`
(
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `openid`      VARCHAR(64)           DEFAULT NULL COMMENT 'wechat openid',
  `name`        VARCHAR(32)           DEFAULT NULL COMMENT 'name',
  `phone`       VARCHAR(32)           DEFAULT NULL COMMENT 'phone number',
  `password`    VARCHAR(128)          DEFAULT NULL COMMENT 'password ({BCRYPT}/{MD5} prefix)',
  `sex`         VARCHAR(2)            DEFAULT NULL COMMENT 'gender',
  `id_number`   VARCHAR(32)           DEFAULT NULL COMMENT 'id card number',
  `avatar`      VARCHAR(255)          DEFAULT NULL COMMENT 'avatar',
  `create_time` DATETIME              DEFAULT NULL COMMENT 'register time',
  PRIMARY KEY (`id`),
  KEY `idx_user_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'user table';

-- 地址簿表
CREATE TABLE IF NOT EXISTS `address_book`
(
  `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `user_id`       BIGINT               DEFAULT NULL COMMENT 'user id',
  `consignee`     VARCHAR(32)          DEFAULT NULL COMMENT 'consignee',
  `phone`         VARCHAR(32)          DEFAULT NULL COMMENT 'phone number',
  `sex`           VARCHAR(2)           DEFAULT NULL COMMENT 'gender',
  `province_code` VARCHAR(16)          DEFAULT NULL COMMENT 'province code',
  `province_name` VARCHAR(32)          DEFAULT NULL COMMENT 'province name',
  `city_code`     VARCHAR(16)          DEFAULT NULL COMMENT 'city code',
  `city_name`     VARCHAR(32)          DEFAULT NULL COMMENT 'city name',
  `district_code` VARCHAR(16)          DEFAULT NULL COMMENT 'district code',
  `district_name` VARCHAR(32)          DEFAULT NULL COMMENT 'district name',
  `detail`        VARCHAR(255)         DEFAULT NULL COMMENT 'detailed address',
  `label`         VARCHAR(16)          DEFAULT NULL COMMENT 'label',
  `is_default`    INT                  DEFAULT 0 COMMENT 'is default 0=no 1=yes',
  PRIMARY KEY (`id`),
  KEY `idx_address_book_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'address book table';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders`
(
  `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `number`                 VARCHAR(64)            DEFAULT NULL COMMENT 'order number',
  `status`                 INT                    DEFAULT NULL COMMENT 'order status 1=pending payment 2=pending acceptance 3=accepted 4=delivering 5=completed 6=cancelled',
  `user_id`                BIGINT                 DEFAULT NULL COMMENT 'user id',
  `address_book_id`        BIGINT                 DEFAULT NULL COMMENT 'address id',
  `order_time`             DATETIME               DEFAULT NULL COMMENT 'order time',
  `checkout_time`          DATETIME               DEFAULT NULL COMMENT 'checkout time',
  `pay_method`             INT                    DEFAULT NULL COMMENT 'pay method 1=wechat 2=alipay',
  `pay_status`             INT                    DEFAULT NULL COMMENT 'pay status 0=unpaid 1=paid 2=refund',
  `amount`                 DECIMAL(10, 2)         DEFAULT NULL COMMENT 'amount received',
  `remark`                 VARCHAR(255)           DEFAULT NULL COMMENT 'remark',
  `user_name`              VARCHAR(32)            DEFAULT NULL COMMENT 'user name',
  `phone`                  VARCHAR(32)            DEFAULT NULL COMMENT 'phone number',
  `address`                VARCHAR(255)           DEFAULT NULL COMMENT 'address',
  `consignee`              VARCHAR(32)            DEFAULT NULL COMMENT 'consignee',
  `cancel_reason`          VARCHAR(255)           DEFAULT NULL COMMENT 'cancel reason',
  `rejection_reason`       VARCHAR(255)           DEFAULT NULL COMMENT 'rejection reason',
  `cancel_time`            DATETIME               DEFAULT NULL COMMENT 'cancel time',
  `estimated_delivery_time` DATETIME              DEFAULT NULL COMMENT 'estimated delivery time',
  `delivery_status`        INT                    DEFAULT NULL COMMENT 'delivery status 1=immediate 0=specific time',
  `delivery_time`          DATETIME               DEFAULT NULL COMMENT 'delivery time',
  `pack_amount`            INT                    DEFAULT 0 COMMENT 'packing fee',
  `tableware_number`       INT                    DEFAULT 0 COMMENT 'tableware number',
  `tableware_status`       INT                    DEFAULT NULL COMMENT 'tableware status 1=by meal count 0=specific count',
  PRIMARY KEY (`id`),
  KEY `idx_orders_number` (`number`),
  KEY `idx_orders_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'order table';

-- 订单明细表
CREATE TABLE IF NOT EXISTS `order_detail`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `name`        VARCHAR(64)            DEFAULT NULL COMMENT 'name',
  `order_id`    BIGINT                 DEFAULT NULL COMMENT 'order id',
  `dish_id`     BIGINT                 DEFAULT NULL COMMENT 'dish id',
  `setmeal_id`  BIGINT                 DEFAULT NULL COMMENT 'setmeal id',
  `dish_flavor` VARCHAR(64)            DEFAULT NULL COMMENT 'flavor',
  `number`      INT                    DEFAULT NULL COMMENT 'quantity',
  `amount`      DECIMAL(10, 2)         DEFAULT NULL COMMENT 'amount',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT 'image',
  PRIMARY KEY (`id`),
  KEY `idx_order_detail_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'order detail table';

-- 购物车表
CREATE TABLE IF NOT EXISTS `shopping_cart`
(
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `name`        VARCHAR(64)            DEFAULT NULL COMMENT 'name',
  `user_id`     BIGINT                 DEFAULT NULL COMMENT 'user id',
  `dish_id`     BIGINT                 DEFAULT NULL COMMENT 'dish id',
  `setmeal_id`  BIGINT                 DEFAULT NULL COMMENT 'setmeal id',
  `dish_flavor` VARCHAR(64)            DEFAULT NULL COMMENT 'flavor',
  `number`      INT                    DEFAULT NULL COMMENT 'quantity',
  `amount`      DECIMAL(10, 2)         DEFAULT NULL COMMENT 'amount',
  `image`       VARCHAR(255)           DEFAULT NULL COMMENT 'image',
  `create_time` DATETIME               DEFAULT NULL COMMENT 'create time',
  PRIMARY KEY (`id`),
  KEY `idx_shopping_cart_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'shopping cart table';
