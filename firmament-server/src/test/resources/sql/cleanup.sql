-- 集成测试通用清理：清空所有业务表，保证每条测试方法在干净数据库上运行。
-- 顺序无关紧要（测试 schema 未声明外键约束），但按依赖关系给出便于阅读。
DELETE FROM `order_detail`;
DELETE FROM `orders`;
DELETE FROM `shopping_cart`;
DELETE FROM `setmeal_dish`;
DELETE FROM `dish_flavor`;
DELETE FROM `dish`;
DELETE FROM `setmeal`;
DELETE FROM `address_book`;
DELETE FROM `category`;
DELETE FROM `employee`;
DELETE FROM `user`;
