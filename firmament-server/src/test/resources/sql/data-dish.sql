-- 集成测试种子数据：菜品/套餐所需的基础分类与既有菜品（cleanup.sql 已先清空所有表）
-- 菜品分类（id=20）、套餐分类（id=21）
INSERT INTO `category`
(`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
(20, 1, '集成测试-菜品分类', 1, 1, NOW(), NOW(), 1, 1),
(21, 2, '集成测试-套餐分类', 1, 1, NOW(), NOW(), 1, 1);

-- 一条既有起售菜品（id=200，分类20）
INSERT INTO `dish`
(`id`, `name`, `category_id`, `price`, `image`, `description`, `status`,
 `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
(200, '宫保鸡丁-集成测试', 20, 38.00, NULL, '测试菜品', 1, NOW(), NOW(), 1, 1);

-- 该菜品的口味
INSERT INTO `dish_flavor`
(`id`, `dish_id`, `name`, `value`)
VALUES
(2000, 200, '甜辣度', '["微辣","中辣","重辣"]');
