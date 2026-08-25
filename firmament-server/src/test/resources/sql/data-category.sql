-- 集成测试种子数据：分类（cleanup.sql 已先清空所有表，这里只插入）
INSERT INTO `category`
(`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
(10, 1, '菜品分类-测试', 1, 1, NOW(), NOW(), 1, 1),
(11, 1, '菜品分类-禁用', 2, 0, NOW(), NOW(), 1, 1),
(12, 2, '套餐分类-测试', 1, 1, NOW(), NOW(), 1, 1);
