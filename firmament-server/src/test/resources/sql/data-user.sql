-- 集成测试种子数据：C端用户（cleanup.sql 已先清空所有表，这里只插入）
-- 密码 123456，格式为 {BCRYPT} 前缀的 BCrypt 哈希（与员工相同）。
INSERT INTO `user`
(`id`, `openid`, `name`, `phone`, `password`, `sex`, `id_number`, `avatar`, `create_time`)
VALUES
(100, 'it-openid-100', 'test-user', '13900000000',
 '{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe',
 '1', '110101199003070001', NULL, NOW());
