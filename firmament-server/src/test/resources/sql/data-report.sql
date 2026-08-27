-- Report / workspace IT: extra users, dishes, setmeals, and orders at known statuses.
-- Loaded after cleanup + data-employee + data-user + data-dish.

INSERT INTO `user`
(`id`, `openid`, `name`, `phone`, `password`, `sex`, `id_number`, `avatar`, `create_time`)
VALUES
(101, 'it-openid-101', 'report-user-yesterday', '13900000001',
 '{BCRYPT}$2a$10$rfWl8eF4t/I/K4xgc4uL4.dhBSZw.VsoEChZHImpNHE7Rq2SU5yhe',
 '1', '110101199003070002', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

INSERT INTO `dish`
(`id`, `name`, `category_id`, `price`, `image`, `description`, `status`,
 `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
(201, 'off-sale-dish-it', 20, 12.00, NULL, 'disabled-for-overview', 0,
 NOW(), NOW(), 1, 1);

INSERT INTO `setmeal`
(`id`, `category_id`, `name`, `price`, `status`, `description`, `image`,
 `create_time`, `update_time`, `create_user`, `update_user`)
VALUES
(300, 21, 'on-sale-setmeal-it', 88.00, 1, 'enabled-for-overview', NULL,
 NOW(), NOW(), 1, 1),
(301, 21, 'off-sale-setmeal-it', 66.00, 0, 'disabled-for-overview', NULL,
 NOW(), NOW(), 1, 1);

INSERT INTO `address_book`
(`id`, `user_id`, `consignee`, `phone`, `sex`,
 `province_code`, `province_name`, `city_code`, `city_name`,
 `district_code`, `district_name`, `detail`, `label`, `is_default`)
VALUES
(1100, 100, 'Zhang San', '13900000000', '1',
 '110000', 'Beijing', '110100', 'Beijing',
 '110105', 'Chaoyang', 'IT-report-address', 'home', 1);

-- status: 1 pending pay, 2 to-be-confirmed, 3 confirmed, 4 delivering, 5 completed, 6 cancelled
-- pay_status: 0 unpaid, 1 paid, 2 refund
INSERT INTO `orders`
(`id`, `number`, `status`, `user_id`, `address_book_id`, `order_time`, `checkout_time`,
 `pay_method`, `pay_status`, `amount`, `user_name`, `phone`, `address`, `consignee`,
 `pack_amount`, `tableware_number`, `tableware_status`, `delivery_status`)
VALUES
(4001, 'IT-RPT-TODAY-DONE', 5, 100, 1100, NOW(), NOW(),
 1, 1, 100.00, 'test-user', '13900000000', 'Beijing Chaoyang IT-report-address', 'Zhang San',
 0, 0, 1, 1),
(4002, 'IT-RPT-YDAY-DONE', 5, 100, 1100, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
 1, 1, 50.00, 'test-user', '13900000000', 'Beijing Chaoyang IT-report-address', 'Zhang San',
 0, 0, 1, 1),
(4003, 'IT-RPT-TODAY-CANCEL', 6, 100, 1100, NOW(), NOW(),
 1, 2, 30.00, 'test-user', '13900000000', 'Beijing Chaoyang IT-report-address', 'Zhang San',
 0, 0, 1, 1),
(4004, 'IT-RPT-TODAY-WAIT', 2, 100, 1100, NOW(), NOW(),
 1, 1, 38.00, 'test-user', '13900000000', 'Beijing Chaoyang IT-report-address', 'Zhang San',
 0, 0, 1, 1),
(4005, 'IT-RPT-TODAY-CONFIRMED', 3, 100, 1100, NOW(), NOW(),
 1, 1, 38.00, 'test-user', '13900000000', 'Beijing Chaoyang IT-report-address', 'Zhang San',
 0, 0, 1, 1);

INSERT INTO `order_detail`
(`id`, `name`, `order_id`, `dish_id`, `setmeal_id`, `dish_flavor`, `number`, `amount`, `image`)
VALUES
(5001, 'it-top-dish', 4001, 200, NULL, NULL, 3, 38.00, NULL),
(5002, 'it-top-dish', 4002, 200, NULL, NULL, 1, 38.00, NULL),
(5003, 'it-second-dish', 4002, 201, NULL, NULL, 2, 12.00, NULL);
