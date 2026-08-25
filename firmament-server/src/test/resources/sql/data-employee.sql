-- 集成测试种子数据：员工（cleanup.sql 已先清空所有表，这里只插入）
-- 密码均为 123456，格式为 {BCRYPT} 前缀的 BCrypt 哈希。
-- 该哈希由 BCryptPasswordEncoder.encode("123456") 生成，cost=10。
INSERT INTO `employee`
(`id`, `username`, `name`, `password`, `phone`, `sex`, `id_number`, `status`,
 `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
(1, 'admin', 'admin',
 '{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe',
 '13800138000', '1', '110101199003071234', 1,
 NOW(), NOW(), 1, 1),
(2, 'second', 'second',
 '{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe',
 '13800138001', '1', '110101199003072345', 1,
 NOW(), NOW(), 1, 1);
